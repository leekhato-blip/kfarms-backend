package com.kfarms.settings.service;

import com.kfarms.entity.AppUser;
import com.kfarms.repository.AppUserRepository;
import com.kfarms.settings.dto.ChangePasswordRequest;
import com.kfarms.settings.dto.OrganizationSettingsDto;
import com.kfarms.settings.dto.UserPreferencesDto;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.entity.TenantPlan;
import com.kfarms.tenant.entity.TenantMember;
import com.kfarms.tenant.repository.TenantMemberRepository;
import com.kfarms.tenant.repository.TenantRepository;
import com.kfarms.tenant.service.TenantContext;
import com.kfarms.tenant.service.TenantPlanGuardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private static final String DEFAULT_TIMEZONE = "Africa/Lagos";
    private static final String DEFAULT_CURRENCY = "NGN";
    private static final String DEFAULT_BRAND_PRIMARY = "#2563EB";
    private static final String DEFAULT_BRAND_ACCENT = "#10B981";
    private static final int DEFAULT_SESSION_TIMEOUT_MINUTES = 480;
    private static final String DEFAULT_THEME_PREFERENCE = "SYSTEM";
    private static final String DEFAULT_LANDING_PAGE = "/dashboard";

    private static final Set<String> ALLOWED_THEME_PREFERENCES = Set.of(
            "SYSTEM",
            "LIGHT",
            "DARK"
    );

    private static final Set<String> ALLOWED_LANDING_PAGES = Set.of(
            "/dashboard",
            "/sales",
            "/supplies",
            "/fish-ponds",
            "/poultry",
            "/livestock",
            "/feeds",
            "/productions",
            "/inventory",
            "/settings",
            "/billing",
            "/support"
    );

    private final TenantRepository tenantRepository;
    private final TenantMemberRepository tenantMemberRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantPlanGuardService tenantPlanGuardService;

    public OrganizationSettingsDto getOrganizationSettings() {
        return toOrganizationSettingsDto(requireTenant());
    }

    public OrganizationSettingsDto updateOrganizationSettings(OrganizationSettingsDto request) {
        Tenant tenant = requireTenant();
        boolean enterprisePlan = tenantPlanGuardService.isAtLeast(tenant.getPlan(), TenantPlan.ENTERPRISE);

        tenant.setName(requireText(request.organizationName(), "Workspace name is required."));
        tenant.setTimezone(orDefault(request.timezone(), DEFAULT_TIMEZONE));
        tenant.setCurrency(orDefault(request.currency(), DEFAULT_CURRENCY).toUpperCase());
        tenant.setContactEmail(emptyToNull(request.contactEmail()));
        tenant.setContactPhone(emptyToNull(request.contactPhone()));
        tenant.setAddress(emptyToNull(request.address()));
        tenant.setWatermarkEnabled(resolveBoolean(
                request.watermarkEnabled(),
                tenant.getWatermarkEnabled(),
                true
        ));
        if (enterprisePlan) {
            tenant.setLogoUrl(emptyToNull(request.logoUrl()));
            tenant.setBrandPrimaryColor(normalizeColor(request.brandPrimaryColor(), DEFAULT_BRAND_PRIMARY));
            tenant.setBrandAccentColor(normalizeColor(request.brandAccentColor(), DEFAULT_BRAND_ACCENT));
            tenant.setLoginHeadline(emptyToNull(request.loginHeadline()));
            tenant.setLoginMessage(emptyToNull(request.loginMessage()));
            tenant.setReportFooter(emptyToNull(request.reportFooter()));
            tenant.setCustomDomain(normalizeDomain(request.customDomain()));
            tenant.setGoogleWorkspaceSsoEnabled(resolveBoolean(
                    request.googleWorkspaceSsoEnabled(),
                    tenant.getGoogleWorkspaceSsoEnabled(),
                    false
            ));
            tenant.setMicrosoftEntraSsoEnabled(resolveBoolean(
                    request.microsoftEntraSsoEnabled(),
                    tenant.getMicrosoftEntraSsoEnabled(),
                    false
            ));
            tenant.setStrongPasswordPolicyEnabled(resolveBoolean(
                    request.strongPasswordPolicyEnabled(),
                    tenant.getStrongPasswordPolicyEnabled(),
                    false
            ));
            tenant.setSessionTimeoutMinutes(resolveInteger(
                    request.sessionTimeoutMinutes(),
                    tenant.getSessionTimeoutMinutes(),
                    DEFAULT_SESSION_TIMEOUT_MINUTES,
                    30,
                    1440
            ));
        }

        return toOrganizationSettingsDto(tenantRepository.save(tenant));
    }

    public UserPreferencesDto getUserPreferences(Authentication authentication) {
        return toUserPreferencesDto(requireMembership(authentication));
    }

    public UserPreferencesDto updateUserPreferences(Authentication authentication, UserPreferencesDto request) {
        TenantMember membership = requireMembership(authentication);

        membership.setThemePreference(normalizeThemePreference(request.themePreference()));
        membership.setLandingPage(normalizeLandingPage(request.landingPage()));
        membership.setEmailNotifications(resolveBoolean(
                request.emailNotifications(),
                membership.getEmailNotifications(),
                true
        ));
        membership.setPushNotifications(resolveBoolean(
                request.pushNotifications(),
                membership.getPushNotifications(),
                true
        ));
        membership.setWeeklySummary(resolveBoolean(
                request.weeklySummary(),
                membership.getWeeklySummary(),
                true
        ));
        membership.setCompactTables(resolveBoolean(
                request.compactTables(),
                membership.getCompactTables(),
                false
        ));

        return toUserPreferencesDto(tenantMemberRepository.save(membership));
    }

    public void updatePassword(Authentication authentication, ChangePasswordRequest request) {
        AppUser user = requireUser(authentication);

        String currentPassword = valueOrEmpty(request.currentPassword());
        String newPassword = valueOrEmpty(request.newPassword());
        String confirmPassword = valueOrEmpty(request.confirmPassword());

        if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            throw new IllegalArgumentException("Please complete all password fields.");
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        Tenant tenant = requireTenant();
        boolean strongPasswordPolicy = Boolean.TRUE.equals(tenant.getStrongPasswordPolicyEnabled());
        int minimumLength = strongPasswordPolicy ? 12 : 8;

        if (newPassword.length() < minimumLength) {
            throw new IllegalArgumentException("New password must be at least " + minimumLength + " characters.");
        }

        if (strongPasswordPolicy && !matchesStrongPasswordPolicy(newPassword)) {
            throw new IllegalArgumentException(
                    "Strong password policy requires upper, lower, number, and special character."
            );
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirmation do not match.");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from current password.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        appUserRepository.save(user);
    }

    private OrganizationSettingsDto toOrganizationSettingsDto(Tenant tenant) {
        return new OrganizationSettingsDto(
                safe(tenant.getName()),
                safe(tenant.getSlug()),
                orDefault(tenant.getTimezone(), DEFAULT_TIMEZONE),
                orDefault(tenant.getCurrency(), DEFAULT_CURRENCY).toUpperCase(),
                safe(tenant.getContactEmail()),
                safe(tenant.getContactPhone()),
                safe(tenant.getAddress()),
                resolveBoolean(tenant.getWatermarkEnabled(), null, true),
                safe(tenant.getLogoUrl()),
                normalizeColor(tenant.getBrandPrimaryColor(), DEFAULT_BRAND_PRIMARY),
                normalizeColor(tenant.getBrandAccentColor(), DEFAULT_BRAND_ACCENT),
                safe(tenant.getLoginHeadline()),
                safe(tenant.getLoginMessage()),
                safe(tenant.getReportFooter()),
                safe(tenant.getCustomDomain()),
                resolveBoolean(tenant.getGoogleWorkspaceSsoEnabled(), null, false),
                resolveBoolean(tenant.getMicrosoftEntraSsoEnabled(), null, false),
                resolveBoolean(tenant.getStrongPasswordPolicyEnabled(), null, false),
                resolveInteger(tenant.getSessionTimeoutMinutes(), null, DEFAULT_SESSION_TIMEOUT_MINUTES, 30, 1440)
        );
    }

    private UserPreferencesDto toUserPreferencesDto(TenantMember membership) {
        return new UserPreferencesDto(
                normalizeThemePreference(membership.getThemePreference()),
                normalizeLandingPage(membership.getLandingPage()),
                resolveBoolean(membership.getEmailNotifications(), null, true),
                resolveBoolean(membership.getPushNotifications(), null, true),
                resolveBoolean(membership.getWeeklySummary(), null, true),
                resolveBoolean(membership.getCompactTables(), null, false)
        );
    }

    private Tenant requireTenant() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Missing tenant context.");
        }

        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found."));
    }

    private AppUser requireUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("User not authenticated.");
        }

        String identity = authentication.getName();
        return appUserRepository.findByEmail(identity)
                .or(() -> appUserRepository.findByUsername(identity))
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private TenantMember requireMembership(Authentication authentication) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Missing tenant context.");
        }

        AppUser user = requireUser(authentication);
        return tenantMemberRepository.findByTenant_IdAndUser_IdAndActiveTrue(tenantId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant membership not found."));
    }

    private String normalizeThemePreference(String value) {
        String normalized = valueOrEmpty(value).trim().toUpperCase();
        if (!ALLOWED_THEME_PREFERENCES.contains(normalized)) {
            return DEFAULT_THEME_PREFERENCE;
        }
        return normalized;
    }

    private String normalizeLandingPage(String value) {
        String normalized = valueOrEmpty(value).trim();
        if ("/livestock".equals(normalized)) {
            normalized = "/poultry";
        }
        if (!ALLOWED_LANDING_PAGES.contains(normalized)) {
            return DEFAULT_LANDING_PAGE;
        }
        return normalized;
    }

    private String requireText(String value, String message) {
        String normalized = valueOrEmpty(value).trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String orDefault(String value, String fallback) {
        String normalized = valueOrEmpty(value).trim();
        return normalized.isBlank() ? fallback : normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalizeColor(String value, String fallback) {
        String normalized = valueOrEmpty(value).trim();
        if (!normalized.matches("^#?[0-9A-Fa-f]{6}$")) {
            return fallback;
        }
        return normalized.startsWith("#") ? normalized.toUpperCase() : ("#" + normalized.toUpperCase());
    }

    private String normalizeDomain(String value) {
        String normalized = valueOrEmpty(value).trim().toLowerCase();
        return normalized.isBlank() ? null : normalized;
    }

    private String emptyToNull(String value) {
        String normalized = valueOrEmpty(value).trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean resolveBoolean(Boolean candidate, Boolean current, boolean fallback) {
        if (candidate != null) {
            return candidate;
        }
        if (current != null) {
            return current;
        }
        return fallback;
    }

    private int resolveInteger(Integer candidate, Integer current, int fallback, int min, int max) {
        int value = candidate != null ? candidate : (current != null ? current : fallback);
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private boolean matchesStrongPasswordPolicy(String value) {
        return value.matches(".*[A-Z].*")
                && value.matches(".*[a-z].*")
                && value.matches(".*\\d.*")
                && value.matches(".*[^A-Za-z0-9].*");
    }
}
