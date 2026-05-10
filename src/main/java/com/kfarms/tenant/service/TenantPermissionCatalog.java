package com.kfarms.tenant.service;

import com.kfarms.tenant.entity.TenantRole;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TenantPermissionCatalog {

    public static final String SETTINGS_VIEW = "SETTINGS_VIEW";
    public static final String SETTINGS_MANAGE = "SETTINGS_MANAGE";
    public static final String USERS_VIEW = "USERS_VIEW";
    public static final String USERS_MANAGE = "USERS_MANAGE";
    public static final String AUDIT_VIEW = "AUDIT_VIEW";
    public static final String ENTERPRISE_VIEW = "ENTERPRISE_VIEW";
    public static final String ENTERPRISE_MANAGE = "ENTERPRISE_MANAGE";
    public static final String REPORT_EXPORT = "REPORT_EXPORT";
    public static final String BILLING_VIEW = "BILLING_VIEW";
    public static final String BILLING_MANAGE = "BILLING_MANAGE";

    public static final List<String> ALL = List.of(
            SETTINGS_VIEW,
            SETTINGS_MANAGE,
            USERS_VIEW,
            USERS_MANAGE,
            AUDIT_VIEW,
            ENTERPRISE_VIEW,
            ENTERPRISE_MANAGE,
            REPORT_EXPORT,
            BILLING_VIEW,
            BILLING_MANAGE
    );

    private static final Map<TenantRole, Set<String>> DEFAULTS = defaults();

    private TenantPermissionCatalog() {
    }

    public static LinkedHashSet<String> defaultPermissions(TenantRole role) {
        TenantRole resolvedRole = role == null ? TenantRole.STAFF : role;
        return new LinkedHashSet<>(DEFAULTS.getOrDefault(resolvedRole, DEFAULTS.get(TenantRole.STAFF)));
    }

    public static LinkedHashSet<String> allPermissions() {
        return new LinkedHashSet<>(ALL);
    }

    public static String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return ALL.contains(normalized) ? normalized : "";
    }

    public static LinkedHashSet<String> normalizeAll(Collection<String> values) {
        LinkedHashSet<String> resolved = new LinkedHashSet<>();
        if (values == null) {
            return resolved;
        }
        for (String value : values) {
            String normalized = normalize(value);
            if (!normalized.isBlank()) {
                resolved.add(normalized);
            }
        }
        return resolved;
    }

    private static Map<TenantRole, Set<String>> defaults() {
        Map<TenantRole, Set<String>> defaults = new LinkedHashMap<>();
        defaults.put(TenantRole.OWNER, allPermissions());
        defaults.put(TenantRole.ADMIN, allPermissions());
        defaults.put(TenantRole.MANAGER, Set.of(
                SETTINGS_VIEW,
                USERS_VIEW,
                AUDIT_VIEW,
                ENTERPRISE_VIEW,
                REPORT_EXPORT,
                BILLING_VIEW
        ));
        defaults.put(TenantRole.STAFF, Set.of(
                SETTINGS_VIEW,
                REPORT_EXPORT
        ));
        return defaults;
    }
}
