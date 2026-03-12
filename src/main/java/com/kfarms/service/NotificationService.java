package com.kfarms.service;

import com.kfarms.dto.NotificationResponseDto;
import com.kfarms.entity.AppUser;
import com.kfarms.entity.Notification;

import java.util.List;

public interface NotificationService {

    /**
     * Create a notification.
     * If user is null => create a global/system notification visible to everyone.
     * If user is non-null => user-specific notification.
     */
    default void createNotification(Long tenantId, String type, String title, String message, AppUser user) {

    }

    List<NotificationResponseDto> getAllNotification();
    List<NotificationResponseDto> getUnreadNotification();
    List<NotificationResponseDto> getUnreadNotificationByUser(Long userId);
    void markAsRead(Long id);
    void markMultipleAsRead(List<Long> ids);
    List<NotificationResponseDto> getNotificationForUser(Long userId);
    NotificationResponseDto toResponse(Notification notification, boolean read);
}
