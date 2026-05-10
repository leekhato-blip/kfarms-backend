package com.kfarms.service.impl;

import com.kfarms.entity.Notification;
import com.kfarms.entity.NotificationType;
import com.kfarms.tenant.entity.Tenant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class CriticalSmsAlertService {

    private static final int MAX_SMS_LENGTH = 280;

    private static final List<String> CRITICAL_KEYWORDS = List.of(
            "critical",
            "urgent",
            "high mortality",
            "mortality",
            "disease",
            "outbreak",
            "stress",
            "water change due",
            "water change",
            "oxygen",
            "low fish stock",
            "low livestock",
            "low feed",
            "low stock alert",
            "low supply stock",
            "payment failed",
            "suspended"
    );

    private static final List<String> WATCH_KEYWORDS = List.of(
            "low stock",
            "running low",
            "below normal",
            "below 50",
            "below 100"
    );

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${kfarms.sms.enabled:false}")
    private boolean enabled;

    @Value("${kfarms.sms.webhook-url:}")
    private String webhookUrl;

    @Value("${kfarms.sms.api-key:}")
    private String apiKey;

    @Value("${kfarms.sms.sender:KFarms}")
    private String sender;

    @Value("${kfarms.sms.preview-logging-enabled:true}")
    private boolean previewLoggingEnabled;

    public void sendIfEligible(Notification notification) {
        if (notification == null || notification.getTenant() == null) {
            return;
        }

        Tenant tenant = notification.getTenant();
        if (!Boolean.TRUE.equals(tenant.getCriticalSmsAlertsEnabled()) || !shouldSend(notification)) {
            return;
        }

        String recipient = normalizePhone(tenant.getContactPhone());
        if (!StringUtils.hasText(recipient)) {
            log.warn("Critical SMS alerts are enabled for tenant {} but no contact phone is configured.", tenant.getId());
            return;
        }

        String smsBody = buildSmsBody(notification, tenant);
        if (!isConfigured()) {
            if (previewLoggingEnabled) {
                log.info("SMS preview for tenant {} to {}: {}", tenant.getId(), recipient, smsBody);
            }
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (StringUtils.hasText(apiKey)) {
                headers.setBearerAuth(apiKey.trim());
            }

            Map<String, Object> payload = Map.of(
                    "to", recipient,
                    "message", smsBody,
                    "sender", StringUtils.hasText(sender) ? sender.trim() : "KFarms",
                    "tenantId", tenant.getId(),
                    "tenantName", safe(tenant.getName()),
                    "notificationId", notification.getId(),
                    "notificationType", notification.getType() != null ? notification.getType().name() : NotificationType.GENERAL.name(),
                    "title", safe(notification.getTitle())
            );

            restTemplate.postForEntity(webhookUrl.trim(), new HttpEntity<>(payload, headers), String.class);
        } catch (Exception ex) {
            log.warn(
                    "SMS dispatch failed for tenant {} notification {}: {}",
                    tenant.getId(),
                    notification.getId(),
                    ex.getMessage()
            );
        }
    }

    private boolean isConfigured() {
        return enabled && StringUtils.hasText(webhookUrl);
    }

    private boolean shouldSend(Notification notification) {
        String content = (safe(notification.getTitle()) + " " + safe(notification.getMessage()))
                .toLowerCase(Locale.ROOT);
        if (containsAny(content, CRITICAL_KEYWORDS)) {
            return true;
        }

        NotificationType type = notification.getType();
        return (type == NotificationType.FEED
                || type == NotificationType.FISH
                || type == NotificationType.LIVESTOCK
                || type == NotificationType.INVENTORY
                || type == NotificationType.GENERAL)
                && containsAny(content, WATCH_KEYWORDS);
    }

    private boolean containsAny(String content, List<String> keywords) {
        return keywords.stream().anyMatch(content::contains);
    }

    private String buildSmsBody(Notification notification, Tenant tenant) {
        String summary = safe(notification.getTitle());
        if (StringUtils.hasText(notification.getMessage()) && !notification.getMessage().equalsIgnoreCase(summary)) {
            summary = StringUtils.hasText(summary) ? summary + ": " + safe(notification.getMessage()) : safe(notification.getMessage());
        }
        String message = safe(tenant.getName()) + " alert: " + (StringUtils.hasText(summary) ? summary : "Critical farm alert.");
        return message.length() > MAX_SMS_LENGTH ? message.substring(0, MAX_SMS_LENGTH - 3) + "..." : message;
    }

    private String normalizePhone(String value) {
        return safe(value).replaceAll("\\s+", "");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
