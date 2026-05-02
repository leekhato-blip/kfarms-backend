package com.kfarms.tenant.service;

import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.entity.TenantAuditAction;
import com.kfarms.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantRecordAuditService {

    private final TenantRepository tenantRepository;
    private final TenantAuditLogService tenantAuditLogService;

    public void created(
            Long tenantId,
            String actor,
            String subjectType,
            Long subjectId,
            String targetName,
            String nextValue,
            String description
    ) {
        record(
                tenantId,
                actor,
                TenantAuditAction.RECORD_CREATED,
                subjectType,
                subjectId,
                targetName,
                null,
                nextValue,
                description
        );
    }

    public void updated(
            Long tenantId,
            String actor,
            String subjectType,
            Long subjectId,
            String targetName,
            String previousValue,
            String nextValue,
            String description
    ) {
        record(
                tenantId,
                actor,
                TenantAuditAction.RECORD_UPDATED,
                subjectType,
                subjectId,
                targetName,
                previousValue,
                nextValue,
                description
        );
    }

    public void deleted(
            Long tenantId,
            String actor,
            String subjectType,
            Long subjectId,
            String targetName,
            String previousValue,
            String description
    ) {
        record(
                tenantId,
                actor,
                TenantAuditAction.RECORD_DELETED,
                subjectType,
                subjectId,
                targetName,
                previousValue,
                null,
                description
        );
    }

    private void record(
            Long tenantId,
            String actor,
            TenantAuditAction action,
            String subjectType,
            Long subjectId,
            String targetName,
            String previousValue,
            String nextValue,
            String description
    ) {
        if (tenantId == null) {
            return;
        }

        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            return;
        }

        tenantAuditLogService.record(
                tenant,
                resolveActor(actor),
                action,
                subjectType,
                subjectId,
                targetName,
                null,
                previousValue,
                nextValue,
                description
        );
    }

    private String resolveActor(String actor) {
        if (actor != null && !actor.isBlank()) {
            return actor.trim();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "SYSTEM";
        }

        String principal = authentication.getName();
        return principal != null && !principal.isBlank() ? principal.trim() : "SYSTEM";
    }
}
