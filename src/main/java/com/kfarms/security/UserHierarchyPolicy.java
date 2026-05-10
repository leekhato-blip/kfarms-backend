package com.kfarms.security;

import com.kfarms.entity.Role;
import com.kfarms.tenant.entity.TenantRole;

public final class UserHierarchyPolicy {

    private UserHierarchyPolicy() {
    }

    public static int tenantRank(TenantRole role) {
        TenantRole resolved = role == null ? TenantRole.STAFF : role;
        return switch (resolved) {
            case STAFF -> 100;
            case MANAGER -> 200;
            case ADMIN -> 300;
            case OWNER -> 400;
        };
    }

    public static boolean canManageTenantMember(TenantRole actorRole, TenantRole targetRole) {
        return tenantRank(actorRole) > tenantRank(targetRole);
    }

    public static boolean canAssignTenantRole(TenantRole actorRole, TenantRole targetRole) {
        return tenantRank(actorRole) > tenantRank(targetRole);
    }

    public static boolean isPlatformAdmin(Role role) {
        return role == Role.PLATFORM_ADMIN;
    }
}
