package com.kfarms.settings.controller;

import com.kfarms.entity.ApiResponse;
import com.kfarms.settings.dto.AccountContactUpdateRequest;
import com.kfarms.settings.dto.AccountContactVerificationRequest;
import com.kfarms.settings.dto.ChangePasswordRequest;
import com.kfarms.settings.dto.OrganizationSettingsDto;
import com.kfarms.settings.dto.UserPreferencesDto;
import com.kfarms.settings.service.SettingsService;
import com.kfarms.tenant.service.TenantPermissionCatalog;
import com.kfarms.tenant.service.TenantPermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final TenantPermissionService tenantPermissionService;

    @GetMapping("/organization")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<OrganizationSettingsDto>> getOrganizationSettings(Authentication authentication) {
        tenantPermissionService.requireAnyPermission(
                authentication,
                "You do not have permission to view workspace settings.",
                TenantPermissionCatalog.SETTINGS_VIEW,
                TenantPermissionCatalog.SETTINGS_MANAGE
        );
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Organization settings loaded.",
                        settingsService.getOrganizationSettings()
                )
        );
    }

    @PutMapping("/organization")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<OrganizationSettingsDto>> updateOrganizationSettings(
            Authentication authentication,
            @RequestBody OrganizationSettingsDto request
    ) {
        tenantPermissionService.requirePermission(
                authentication,
                TenantPermissionCatalog.SETTINGS_MANAGE,
                "You do not have permission to update workspace settings."
        );
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Organization settings saved.",
                        settingsService.updateOrganizationSettings(request)
                )
        );
    }

    @GetMapping("/preferences")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<UserPreferencesDto>> getUserPreferences(Authentication authentication) {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Preferences loaded.",
                        settingsService.getUserPreferences(authentication)
                )
        );
    }

    @PutMapping("/preferences")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<UserPreferencesDto>> updateUserPreferences(
            Authentication authentication,
            @RequestBody UserPreferencesDto request
    ) {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Preferences saved.",
                        settingsService.updateUserPreferences(authentication, request)
                )
        );
    }

    @PostMapping("/password")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, String>>> updatePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        settingsService.updatePassword(authentication, request);
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Password updated successfully.",
                        Map.of("status", "updated")
                )
        );
    }

    @GetMapping("/account-contact")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAccountContactStatus(Authentication authentication) {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Account contact status loaded.",
                        settingsService.getAccountContactStatus(authentication)
                )
        );
    }

    @PutMapping("/account-contact")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateAccountContact(
            Authentication authentication,
            @RequestBody AccountContactUpdateRequest request
    ) {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Account contact details updated.",
                        settingsService.updateAccountContact(authentication, request.phoneNumber())
                )
        );
    }

    @PostMapping("/account-contact/send-codes")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendAccountContactCodes(Authentication authentication) {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Verification codes sent.",
                        settingsService.sendAccountContactCodes(authentication)
                )
        );
    }

    @PostMapping("/account-contact/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyAccountContact(
            Authentication authentication,
            @RequestBody AccountContactVerificationRequest request
    ) {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Contact verification completed.",
                        settingsService.verifyAccountContact(authentication, request.emailCode(), request.phoneCode())
                )
        );
    }
}
