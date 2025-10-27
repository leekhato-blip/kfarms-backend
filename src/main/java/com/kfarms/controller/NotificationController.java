package com.kfarms.controller;

import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.AppUser;
import com.kfarms.entity.Notification;
import com.kfarms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // Fetch all notifications (for admin view)
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Notification>>> getAllNotifications() {
        List<Notification> response = notificationService.getAllNotification();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "All notifications fetched", response)
        );
    }

    // Fetch unread notifications
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<Notification>>>  getUnreadNotification() {
        List<Notification> response = notificationService.getUnreadNotification();
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

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        // Cleanup: remove emitter if client disconnects or timeout occurs
        emitter.onCompletion(() -> {
            List<SseEmitter> list = emitters.get(userId);
            if (list != null) list.remove(emitter);
        });
        emitter.onTimeout(() -> {
            List<SseEmitter> list = emitters.get(userId);
            if (list != null) list.remove(emitter);
        });
        return emitter;
    }

    // Send live notification to relevant clients
    private void sendToClients(Notification notification) {
        List<SseEmitter> deadEmitters = new ArrayList<>();

        if (notification.getUser() == null) {
            // Global notifications -> send to all
            emitters.values().forEach(list -> list.forEach(e -> send(e, notification, deadEmitters)));
        } else {
            // Targeted or admin notifications
            Long targetId = notification.getUser().getId();
            emitters.getOrDefault(targetId, List.of())
                    .forEach(e -> send(e, notification, deadEmitters));
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
}
