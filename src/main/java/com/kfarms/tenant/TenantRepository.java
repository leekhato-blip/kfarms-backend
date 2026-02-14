package com.kfarms.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCase(String slug);
}
