package com.kfarms.controller;

import com.kfarms.dto.NotificationResponseDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.Notification;
import com.kfarms.service.NotificationService;
import com.kfarms.tenant.service.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // Holds active SSE connections for all connected clients
    // CopyOnWriteArrayList prevents concurrent modification issues
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // Fetch all notifications (for admin view)
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<NotificationResponseDto>>> getAllNotifications() {
        List<NotificationResponseDto> response = notificationService.getAllNotification();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "All notifications fetched", response)
        );
    }

    // Fetch unread notifications
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponseDto>>>  getUnreadNotification() {
        List<NotificationResponseDto> response = notificationService.getUnreadNotification();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Unread Notification fetched", response)
        );
    }

    // Mark one notification as read
    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);//
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Notification marked as read", null)
        );
    }

    // Mark multiple notifications as read
    @PostMapping("/multiple-read")
    public ResponseEntity<ApiResponse<String>> markMultipleAsRead(@RequestBody List<Long> ids) {
        notificationService.markMultipleAsRead(ids);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Notifications marked as read", null)
        );
    }

    // 🟣 Real-time notifications (SSE)
    @GetMapping(value = "/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotification(@PathVariable Long userId) {
        notificationService.getNotificationForUser(userId);
        Long tenantId = TenantContext.getTenantId();
        String emitterKey = emitterKey(tenantId, userId);

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.computeIfAbsent(emitterKey, id -> new CopyOnWriteArrayList<>()).add(emitter);
        // Cleanup: remove emitter if client disconnects or timeout occurs
        emitter.onCompletion(() -> {
            List<SseEmitter> list = emitters.get(emitterKey);
            if (list != null) list.remove(emitter);
        });
        emitter.onTimeout(() -> {
            List<SseEmitter> list = emitters.get(emitterKey);
            if (list != null) list.remove(emitter);
        });
        return emitter;
    }

    // Send live notification to relevant clients
    private void sendToClients(Notification notification) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        String tenantPrefix = tenantPrefix(notification.getTenant() != null ? notification.getTenant().getId() : null);

        if (notification.getUser() == null) {
            emitters.forEach((key, list) -> {
                if (key.startsWith(tenantPrefix)) {
                    NotificationResponseDto payload = notificationService.toResponse(notification, false);
                    list.forEach(e -> send(e, payload, deadEmitters));
                }
            });
        } else {
            String targetKey = emitterKey(
                    notification.getTenant() != null ? notification.getTenant().getId() : null,
                    notification.getUser().getId()
            );
            NotificationResponseDto payload = notificationService.toResponse(notification, false);
            emitters.getOrDefault(targetKey, List.of())
                    .forEach(e -> send(e, payload, deadEmitters));
        }

        // Clean dead connections
        emitters.forEach((id, list) -> list.removeAll(deadEmitters));
    }

    // helper method
    private void send(SseEmitter emitter, Object data, List<SseEmitter> deadEmitters) {
        try {
            emitter.send(SseEmitter.event().name("notification").data(data));
        } catch (IOException e) {
            deadEmitters.add(emitter); // mark dead emitter for cleanup
        }
    }

    // Event listener for real-time push
    @EventListener
    public void handleNotificationEvent(Notification notification) {
        sendToClients(notification);
    }

    private String emitterKey(Long tenantId, Long userId) {
        return tenantPrefix(tenantId) + (userId == null ? "guest" : userId);
    }

    private String tenantPrefix(Long tenantId) {
        return (tenantId == null ? "0" : tenantId) + ":";
    }
}
