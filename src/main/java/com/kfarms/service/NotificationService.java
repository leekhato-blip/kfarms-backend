package com.kfarms.service;


import com.kfarms.entity.Notification;

import java.util.List;

public interface NotificationService {
    void createNotification(String type, String title, String message);
    List<Notification> getUnreadNotification();
    void markAsRead(Long id);
}
