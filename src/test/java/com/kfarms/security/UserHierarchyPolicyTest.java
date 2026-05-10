package com.kfarms.security;

import com.kfarms.tenant.entity.TenantRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserHierarchyPolicyTest {

    @Test
    void tenantHierarchyOnlyManagesLowerRoles() {
        assertTrue(UserHierarchyPolicy.canManageTenantMember(TenantRole.OWNER, TenantRole.ADMIN));
        assertTrue(UserHierarchyPolicy.canManageTenantMember(TenantRole.ADMIN, TenantRole.MANAGER));
        assertTrue(UserHierarchyPolicy.canManageTenantMember(TenantRole.MANAGER, TenantRole.STAFF));

        assertFalse(UserHierarchyPolicy.canManageTenantMember(TenantRole.ADMIN, TenantRole.ADMIN));
        assertFalse(UserHierarchyPolicy.canManageTenantMember(TenantRole.ADMIN, TenantRole.OWNER));
    }

    @Test
    void tenantHierarchyOnlyAssignsLowerRoles() {
        assertTrue(UserHierarchyPolicy.canAssignTenantRole(TenantRole.OWNER, TenantRole.ADMIN));
        assertTrue(UserHierarchyPolicy.canAssignTenantRole(TenantRole.ADMIN, TenantRole.MANAGER));
        assertFalse(UserHierarchyPolicy.canAssignTenantRole(TenantRole.ADMIN, TenantRole.ADMIN));
        assertFalse(UserHierarchyPolicy.canAssignTenantRole(TenantRole.MANAGER, TenantRole.ADMIN));
    }
}
