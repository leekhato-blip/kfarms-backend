package com.kfarms.tenant.repository;

import com.kfarms.tenant.entity.TenantAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TenantAuditLogRepository
        extends JpaRepository<TenantAuditLog, Long>, JpaSpecificationExecutor<TenantAuditLog> {
}
