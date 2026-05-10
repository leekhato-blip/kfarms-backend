package com.kfarms.tenant.repository;

import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.entity.TenantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long>, JpaSpecificationExecutor<Tenant> {

    Optional<Tenant> findBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCase(String slug);
    long countByStatus(TenantStatus status);
}
