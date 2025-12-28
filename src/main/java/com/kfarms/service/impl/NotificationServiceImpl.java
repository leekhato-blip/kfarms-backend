package com.kfarms.service.impl;

import com.kfarms.entity.AppUser;
import com.kfarms.entity.Notification;
import com.kfarms.entity.NotificationType;
import com.kfarms.entity.Role;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.repository.AppUserRepository;
import com.kfarms.repository.NotificationRepository;
import com.kfarms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Not;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepo;
    private final AppUserRepository userRepo;
    private final ApplicationEventPublisher publisher;

    @Override
    public void createNotification(String type, String title, String message, AppUser user) {
        // Safely convert incoming type string to NotificationType enum
        NotificationType safeType;
        try {
            safeType = type != null ? NotificationType.valueOf(type.toUpperCase()) : NotificationType.GENERAL;
        } catch (IllegalArgumentException e) {
            safeType = NotificationType.GENERAL; // fallback to GENERAL if invalid
        }

        boolean isConfidential = (safeType == NotificationType.FINANCE || safeType == NotificationType.SUPPLIES);

        // Assign only admin to confidential notification
        if (isConfidential && (user == null || user.getRole() != Role.ADMIN)) {
            List<AppUser> admins = userRepo.findByRole(Role.ADMIN);
            for (AppUser admin : admins) {
                saveAndPublish(admin, safeType, title, message);
            }
        } else {
            // global or personal notification
            saveAndPublish(user, safeType, title, message);
        }

    }

    private void saveAndPublish(AppUser user, NotificationType type, String title, String message) {
        Notification entity = Notification.builder()
                .type(type)
                .title(title)
                .message(message)
                .user(user) // null = global
                .createdAt(LocalDateTime.now())
                .build();
        Notification saved = notificationRepo.save(entity);

        // 🔔 Publish event so Controller can broadcast to clients
        // This triggers the controller’s @EventListener method → sends live update via SSE
        publisher.publishEvent(saved);
    }

    // 🟢 Fetch all notifications
    @Override
    public List<Notification> getAllNotification() {
        return notificationRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    // 🔵 Fetch all unread notifications
    @Override
    public List<Notification> getUnreadNotification() {
        return notificationRepo.findByReadFalseOrderByCreatedAtDesc();
    }

    // 🔵 Fetch unread notifications for specific user
    @Override
    public List<Notification> getUnreadNotificationByUser(Long userId) {
        return notificationRepo.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    // 🟡 Mark one notification as read
    @Override
    public void markAsRead(Long id) {
        Notification entity = notificationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        entity.setRead(true);
        notificationRepo.save(entity);
    }

    // 🟡 Mark multiple notifications as read
    @Override
    public void markMultipleAsRead(List<Long> ids) {
        List<Notification> notifications = notificationRepo.findAllById(ids);
        notifications.forEach(n -> n.setRead(true));
        notificationRepo.saveAll(notifications);
    }

    // 🟣 Get notifications visible to a specific user
    @Override
    public List<Notification> getNotificationForUser(Long userId) {
        // Get all notifications visible to user (global + personal)
        List<Notification> visible = notificationRepo.findForUserOrGlobal(userId);

        // Check if user is admin
        AppUser user = visible.stream()
                .filter(n -> n.getUser() != null && n.getUser().getId().equals(userId))
                .map(Notification::getUser)
                .findFirst()
                .orElse(null);

        if (isAdmin(user)) {
            // Admins see everything (global + confidential)
            return notificationRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        } else {
            // Regular users only see global
            return visible.stream()
                    .filter(n -> n.getUser() == null)
                    .toList();
        }
    }

    private boolean isAdmin(AppUser user) {
        return user != null && user.getRole() == Role.ADMIN;
    }
}
