package com.kfarms.platform.service;

import com.kfarms.entity.NotificationType;
import com.kfarms.platform.dto.PlatformAnnouncementRequest;
import com.kfarms.service.NotificationService;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.entity.TenantStatus;
import com.kfarms.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlatformAnnouncementServiceImpl implements PlatformAnnouncementService {

    private static final String ALL_ACTIVE_AUDIENCE = "ALL_ACTIVE";
    private static final String SPECIFIC_AUDIENCE = "SPECIFIC";
    private static final String ANNOUNCEMENT_TITLE_PREFIX = "Announcement:";

    private final TenantRepository tenantRepository;
    private final NotificationService notificationService;

    @Override
    public Map<String, Object> sendAnnouncement(PlatformAnnouncementRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Announcement details are required.");
        }

        String normalizedTitle = normalizeText(request.getTitle());
        String normalizedMessage = normalizeText(request.getMessage());

        if (normalizedTitle.isBlank() || normalizedMessage.isBlank()) {
            throw new IllegalArgumentException("Announcement title and message are required.");
        }

        String normalizedAudience = normalizeAudience(request.getAudience());
        List<Tenant> targets = resolveTargets(normalizedAudience, request.getTenantIds());

        if (targets.isEmpty()) {
            throw new IllegalArgumentException("No active workspaces are available for this announcement.");
        }

        String storedTitle = formatStoredTitle(normalizedTitle);

        for (Tenant tenant : targets) {
            notificationService.createNotification(
                    tenant.getId(),
                    NotificationType.SYSTEM.name(),
                    storedTitle,
                    normalizedMessage,
                    null
            );
        }

        return Map.of(
                "audience", normalizedAudience,
                "tenantCount", targets.size(),
                "tenantIds", targets.stream().map(Tenant::getId).toList()
        );
    }

    private String normalizeAudience(String audience) {
        String normalized = normalizeText(audience).toUpperCase();
        if (normalized.isBlank()) {
            return ALL_ACTIVE_AUDIENCE;
        }
        if (!ALL_ACTIVE_AUDIENCE.equals(normalized) && !SPECIFIC_AUDIENCE.equals(normalized)) {
            throw new IllegalArgumentException("Audience must be ALL_ACTIVE or SPECIFIC.");
        }
        return normalized;
    }

    private List<Tenant> resolveTargets(String audience, List<Long> requestedTenantIds) {
        if (SPECIFIC_AUDIENCE.equals(audience)) {
            LinkedHashSet<Long> tenantIds = new LinkedHashSet<>(
                    requestedTenantIds == null ? List.of() : requestedTenantIds
            );

            if (tenantIds.isEmpty()) {
                throw new IllegalArgumentException("Choose at least one workspace for a targeted announcement.");
            }

            List<Tenant> tenants = tenantRepository.findAllById(tenantIds).stream()
                    .filter(tenant -> tenant.getStatus() == TenantStatus.ACTIVE)
                    .toList();

            if (tenants.isEmpty()) {
                throw new IllegalArgumentException("None of the selected workspaces are active.");
            }

            return tenants;
        }

        return tenantRepository.findAll().stream()
                .filter(tenant -> tenant.getStatus() == TenantStatus.ACTIVE)
                .toList();
    }

    private String formatStoredTitle(String title) {
        if (title.toLowerCase().startsWith(ANNOUNCEMENT_TITLE_PREFIX.toLowerCase())) {
            return title;
        }
        return ANNOUNCEMENT_TITLE_PREFIX + " " + title;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
