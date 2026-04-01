package com.kfarms.service.impl;

import com.kfarms.dto.NotificationResponseDto;
import com.kfarms.entity.AppUser;
import com.kfarms.entity.Notification;
import com.kfarms.entity.NotificationReadState;
import com.kfarms.entity.NotificationType;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.repository.AppUserRepository;
import com.kfarms.repository.NotificationRepository;
import com.kfarms.repository.NotificationReadStateRepository;
import com.kfarms.service.NotificationService;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.repository.TenantRepository;
import com.kfarms.tenant.service.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Duration DUPLICATE_SUPPRESSION_WINDOW = Duration.ofHours(12);

    private final NotificationRepository notificationRepo;
    private final NotificationReadStateRepository notificationReadStateRepo;
    private final AppUserRepository userRepo;
    private final TenantRepository tenantRepository;
    private final ApplicationEventPublisher publisher;

    @Override
    public void createNotification(Long tenantId, String type, String title, String message, AppUser user) {
        if (tenantId == null) {
            return;
        }

        NotificationType safeType = resolveNotificationType(type);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));
        saveAndPublish(tenant, user, safeType, title, message);
    }

    private NotificationType resolveNotificationType(String type) {
        String normalized = type != null ? type.trim().toUpperCase() : "";

        return switch (normalized) {
            case "SUPPLIES" -> NotificationType.INVENTORY;
            case "POULTRY", "BIRDS" -> NotificationType.LIVESTOCK;
            case "SALES", "REVENUE" -> NotificationType.FINANCE;
            case "FEED" -> NotificationType.FEED;
            case "FISH" -> NotificationType.FISH;
            case "LIVESTOCK" -> NotificationType.LIVESTOCK;
            case "LAYER" -> NotificationType.LAYER;
            case "FINANCE" -> NotificationType.FINANCE;
            case "INVENTORY" -> NotificationType.INVENTORY;
            case "SYSTEM" -> NotificationType.SYSTEM;
            case "GENERAL" -> NotificationType.GENERAL;
            default -> NotificationType.GENERAL;
        };
    }

    private void saveAndPublish(Tenant tenant, AppUser user, NotificationType type, String title, String message) {
        String safeTitle = title == null ? "" : title.trim();
        String safeMessage = message == null ? "" : message.trim();

        if (safeTitle.isBlank() && safeMessage.isBlank()) {
            return;
        }

        if (isRecentDuplicate(tenant, user, type, safeTitle, safeMessage)) {
            return;
        }

        Notification entity = Notification.builder()
                .type(type)
                .title(safeTitle)
                .message(safeMessage)
                .user(user) // null = global
                .tenant(tenant)
                .createdAt(LocalDateTime.now())
                .build();
        Notification saved = notificationRepo.save(entity);

        // 🔔 Publish event so Controller can broadcast to clients
        // This triggers the controller’s @EventListener method → sends live update via SSE
        publisher.publishEvent(saved);
    }

    // 🟢 Fetch all notifications
    @Override
    public List<NotificationResponseDto> getAllNotification() {
        return toResponses(
                notificationRepo.findAllByTenant_IdOrderByCreatedAtDesc(requireTenantId()),
                currentUser()
        );
    }

    // 🔵 Fetch all unread notifications
    @Override
    public List<NotificationResponseDto> getUnreadNotification() {
        Long tenantId = requireTenantId();
        AppUser user = currentUser();
        if (user == null) {
            return notificationRepo.findByTenant_IdAndReadFalseOrderByCreatedAtDesc(tenantId)
                    .stream()
                    .map(notification -> toResponse(notification, Boolean.TRUE.equals(notification.getRead())))
                    .toList();
        }
        return toUnreadResponses(
                notificationRepo.findForUserOrGlobal(tenantId, user.getId()),
                user
        );
    }

    // 🔵 Fetch unread notifications for specific user
    @Override
    public List<NotificationResponseDto> getUnreadNotificationByUser(Long userId) {
        return toUnreadResponses(
                notificationRepo.findForUserOrGlobal(requireTenantId(), userId),
                requireVisibleUser(userId)
        );
    }

    // 🟡 Mark one notification as read
    @Override
    public void markAsRead(Long id) {
        AppUser viewer = requireCurrentUser();
        Notification notification = requireVisibleNotification(id, viewer);
        markNotificationRead(notification, viewer);
    }

    // 🟡 Mark multiple notifications as read
    @Override
    public void markMultipleAsRead(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        AppUser viewer = requireCurrentUser();
        List<Notification> notifications = notificationRepo.findByIdInAndTenant_Id(ids, requireTenantId())
                .stream()
                .filter(notification -> isVisibleToUser(notification, viewer.getId()))
                .toList();

        notifications.forEach(notification -> markNotificationRead(notification, viewer));
    }

    // 🟣 Get notifications visible to a specific user
    @Override
    public List<NotificationResponseDto> getNotificationForUser(Long userId) {
        return toResponses(
                notificationRepo.findForUserOrGlobal(requireTenantId(), userId),
                requireVisibleUser(userId)
        );
    }

    @Override
    public NotificationResponseDto toResponse(Notification notification, boolean read) {
        return new NotificationResponseDto(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                read,
                notification.getUser() != null,
                notification.getCreatedAt()
        );
    }

    private boolean isRecentDuplicate(Tenant tenant, AppUser user, NotificationType type, String title, String message) {
        LocalDateTime since = LocalDateTime.now().minus(DUPLICATE_SUPPRESSION_WINDOW);
        if (user == null) {
            return notificationRepo.existsRecentGlobalDuplicate(
                    tenant.getId(),
                    type,
                    title,
                    message,
                    since
            );
        }

        return notificationRepo.existsRecentUserDuplicate(
                tenant.getId(),
                user.getId(),
                type,
                title,
                message,
                since
        );
    }

    private List<NotificationResponseDto> toUnreadResponses(List<Notification> notifications, AppUser viewer) {
        Set<Long> readIds = resolveReadIds(notifications, viewer);
        return notifications.stream()
                .filter(notification -> !isReadForViewer(notification, viewer, readIds))
                .map(notification -> toResponse(notification, false))
                .toList();
    }

    private List<NotificationResponseDto> toResponses(List<Notification> notifications, AppUser viewer) {
        Set<Long> readIds = resolveReadIds(notifications, viewer);
        return notifications.stream()
                .map(notification -> toResponse(
                        notification,
                        isReadForViewer(notification, viewer, readIds)
                ))
                .toList();
    }

    private Set<Long> resolveReadIds(Collection<Notification> notifications, AppUser viewer) {
        if (viewer == null || notifications == null || notifications.isEmpty()) {
            return Set.of();
        }

        List<Long> notificationIds = notifications.stream()
                .map(Notification::getId)
                .filter(id -> id != null)
                .toList();

        if (notificationIds.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(notificationReadStateRepo.findReadNotificationIds(viewer.getId(), notificationIds));
    }

    private boolean isReadForViewer(Notification notification, AppUser viewer, Set<Long> readIds) {
        if (notification == null) {
            return true;
        }
        if (viewer == null) {
            return Boolean.TRUE.equals(notification.getRead());
        }
        if (Boolean.TRUE.equals(notification.getRead())) {
            return true;
        }
        return readIds.contains(notification.getId());
    }

    private Notification requireVisibleNotification(Long id, AppUser viewer) {
        Notification notification = notificationRepo.findByIdAndTenant_Id(id, requireTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));

        if (!isVisibleToUser(notification, viewer.getId())) {
            throw new AccessDeniedException("Notification is not visible to this user.");
        }

        return notification;
    }

    private boolean isVisibleToUser(Notification notification, Long userId) {
        return notification.getUser() == null
                || (notification.getUser() != null && notification.getUser().getId().equals(userId));
    }

    private void markNotificationRead(Notification notification, AppUser viewer) {
        if (notificationReadStateRepo.existsByNotification_IdAndUser_Id(notification.getId(), viewer.getId())) {
            return;
        }

        notificationReadStateRepo.save(NotificationReadState.builder()
                .notification(notification)
                .user(viewer)
                .readAt(LocalDateTime.now())
                .build());

        if (notification.getUser() != null && notification.getUser().getId().equals(viewer.getId())
                && !Boolean.TRUE.equals(notification.getRead())) {
            notification.setRead(true);
            notificationRepo.save(notification);
        }
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Missing tenant context");
        }
        return tenantId;
    }

    private AppUser requireCurrentUser() {
        AppUser user = currentUser();
        if (user == null) {
            throw new AccessDeniedException("User not authenticated.");
        }
        return user;
    }

    private AppUser requireVisibleUser(Long userId) {
        AppUser viewer = requireCurrentUser();
        if (!viewer.getId().equals(userId)) {
            throw new AccessDeniedException("Cannot inspect notifications for another user.");
        }
        return viewer;
    }

    private AppUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }

        String identity = auth.getName();
        return userRepo.findByEmail(identity)
                .orElseGet(() -> userRepo.findByUsername(identity).orElse(null));
    }
}
