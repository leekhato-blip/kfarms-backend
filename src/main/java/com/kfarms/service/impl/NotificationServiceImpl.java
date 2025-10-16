package com.kfarms.service.impl;

import com.kfarms.entity.Notification;
import com.kfarms.entity.NotificationType;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.repository.NotificationRepository;
import com.kfarms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepo;

    @Override
    public void createNotification(String type, String title, String message) {
        NotificationType safeType;
        try {
            safeType = type != null ? NotificationType.valueOf(type.toUpperCase())  : NotificationType.GENERAL;
        } catch (IllegalArgumentException e) {
            safeType = NotificationType.GENERAL;
        }
        Notification entity = Notification.builder()
                .type(safeType)
                .title(title)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepo.save(entity);
    }

    @Override
    public List<Notification> getAllNotification() {
        return notificationRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
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

    @Override
    public void markMultipleAsRead(List<Long> ids) {
        List<Notification> notifications = notificationRepo.findAllById(ids);
        notifications.forEach(n -> n.setRead(true));
        notificationRepo.saveAll(notifications);
    }
}
