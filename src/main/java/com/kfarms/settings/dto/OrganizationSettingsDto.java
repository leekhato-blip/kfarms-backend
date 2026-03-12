package com.kfarms.settings.dto;

public record OrganizationSettingsDto(
        String organizationName,
        String organizationSlug,
        String timezone,
        String currency,
        String contactEmail,
        String contactPhone,
        String address,
        Boolean watermarkEnabled,
        String logoUrl,
        String brandPrimaryColor,
        String brandAccentColor,
        String loginHeadline,
        String loginMessage,
        String reportFooter,
        String customDomain,
        Boolean googleWorkspaceSsoEnabled,
        Boolean microsoftEntraSsoEnabled,
        Boolean strongPasswordPolicyEnabled,
        Integer sessionTimeoutMinutes
) {
}
