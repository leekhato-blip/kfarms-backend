package com.kfarms.tenant.service;

import com.kfarms.entity.AppUser;
import com.kfarms.repository.AppUserRepository;
import com.kfarms.tenant.entity.TenantMember;
import com.kfarms.tenant.entity.TenantRole;
import com.kfarms.tenant.repository.TenantMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service("tenantPermissionService")
@RequiredArgsConstructor
public class TenantPermissionService {

    private final AppUserRepository appUserRepository;
    private final TenantMemberRepository tenantMemberRepository;

    public List<String> resolvePermissionsForResponse(TenantMember member) {
        return List.copyOf(resolvePermissions(member));
    }

    public String resolveRoleLabel(TenantMember member) {
        if (member == null) {
            return TenantRole.STAFF.name();
        }
        String customRoleName = member.getCustomRoleName();
        if (customRoleName != null && !customRoleName.isBlank()) {
            return customRoleName.trim();
        }
        return member.getRole() != null ? member.getRole().name() : TenantRole.STAFF.name();
    }

    public LinkedHashSet<String> resolvePermissions(TenantMember member) {
        if (member == null) {
            return TenantPermissionCatalog.defaultPermissions(TenantRole.STAFF);
        }
        if (member.getRole() == TenantRole.OWNER) {
            return TenantPermissionCatalog.allPermissions();
        }
        LinkedHashSet<String> overrides = parseStoredPermissions(member.getPermissionOverrides());
        if (!overrides.isEmpty()) {
            return overrides;
        }
        return TenantPermissionCatalog.defaultPermissions(member.getRole());
    }

    public String serializePermissions(Set<String> permissions, TenantRole role) {
        LinkedHashSet<String> resolved = permissions == null || permissions.isEmpty()
                ? TenantPermissionCatalog.defaultPermissions(role)
                : TenantPermissionCatalog.normalizeAll(permissions);
        return resolved.stream().collect(Collectors.joining(","));
    }

    public LinkedHashSet<String> parseStoredPermissions(String raw) {
        if (raw == null || raw.isBlank()) {
            return new LinkedHashSet<>();
        }
        return TenantPermissionCatalog.normalizeAll(
                Arrays.stream(raw.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .toList()
        );
    }

    public boolean hasPermission(Authentication authentication, String permission) {
        TenantMember member = requireMembership(authentication);
        String normalizedPermission = TenantPermissionCatalog.normalize(permission);
        return !normalizedPermission.isBlank() && resolvePermissions(member).contains(normalizedPermission);
    }

    public boolean hasAnyPermission(Authentication authentication, String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return true;
        }
        TenantMember member = requireMembership(authentication);
        Set<String> granted = resolvePermissions(member);
        for (String permission : permissions) {
            String normalizedPermission = TenantPermissionCatalog.normalize(permission);
            if (!normalizedPermission.isBlank() && granted.contains(normalizedPermission)) {
                return true;
            }
        }
        return false;
    }

    public void requirePermission(Authentication authentication, String permission, String message) {
        if (!hasPermission(authentication, permission)) {
            throw new AccessDeniedException(message);
        }
    }

    public void requireAnyPermission(Authentication authentication, String message, String... permissions) {
        if (!hasAnyPermission(authentication, permissions)) {
            throw new AccessDeniedException(message);
        }
    }

    private TenantMember requireMembership(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated.");
        }

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AccessDeniedException("Missing tenant context.");
        }

        String identity = authentication.getName();
        AppUser user = appUserRepository.findByEmail(identity)
                .or(() -> appUserRepository.findByUsername(identity))
                .orElseThrow(() -> new AccessDeniedException("User not found."));

        return tenantMemberRepository.findByTenant_IdAndUser_IdAndActiveTrue(tenantId, user.getId())
                .orElseThrow(() -> new AccessDeniedException("Tenant membership not found."));
    }
}
