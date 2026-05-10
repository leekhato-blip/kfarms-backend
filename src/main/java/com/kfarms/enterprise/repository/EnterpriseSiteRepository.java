package com.kfarms.enterprise.repository;

import com.kfarms.enterprise.entity.EnterpriseSite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnterpriseSiteRepository extends JpaRepository<EnterpriseSite, Long> {

    List<EnterpriseSite> findByTenant_IdOrderByNameAsc(Long tenantId);

    Optional<EnterpriseSite> findByIdAndTenant_Id(Long id, Long tenantId);
}
