package com.kfarms.controller;

import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.Notification;
import com.kfarms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>>  getUnreadNotification() {
        List<Notification> response = notificationService.getUnreadNotification();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Unread Notification fetched", response)
        );
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Notification marked as read", null)
        );
    }
}
