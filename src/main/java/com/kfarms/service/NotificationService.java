package com.kfarms.service;


import com.kfarms.entity.Notification;
import com.kfarms.entity.NotificationType;

import java.util.List;

public interface NotificationService {
    void createNotification(String type, String title, String message);
    List<Notification> getAllNotification();
    List<Notification> getUnreadNotification();
    void markAsRead(Long id);
    void markMultipleAsRead(List<Long> ids);
}
