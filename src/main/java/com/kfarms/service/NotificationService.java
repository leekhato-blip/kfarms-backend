package com.kfarms.service;

import com.kfarms.entity.AppUser;
import com.kfarms.entity.Notification;

import java.util.List;

public interface NotificationService {

    /**
     * Create a notification.
     * If user is null => create a global/system notification visible to everyone.
     * If user is non-null => user-specific notification.
     */
    void createNotification(String type, String title, String message, AppUser user);

    List<Notification> getAllNotification();
    List<Notification> getUnreadNotification();
    List<Notification> getUnreadNotificationByUser(Long userId);
    void markAsRead(Long id);
    void markMultipleAsRead(List<Long> ids);
    List<Notification> getNotificationForUser(Long userId);
}
