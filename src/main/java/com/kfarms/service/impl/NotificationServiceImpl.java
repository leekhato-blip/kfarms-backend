package com.kfarms.service.impl;

import com.kfarms.entity.Notification;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.repository.NotificationRepository;
import com.kfarms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepo;

    @Override
    public void createNotification(String type, String title, String message) {
        Notification entity = Notification.builder()
                .type(type)
                .title(title)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepo.save(entity);
    }

    @Override
    public List<Notification> getUnreadNotification() {
        return notificationRepo.findByReadFalseOrderByCreatedAtDesc();
    }

    @Override
    public void markAsRead(Long id) {
        Notification entity = notificationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        entity.setRead(true);
        notificationRepo.save(entity);
    }
}
