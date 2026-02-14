package com.kfarms.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TenantMemberRepository extends JpaRepository<TenantMember, Long> {

    Optional<TenantMember> findByTenant_IdAndUser_IdAndActiveTrue(Long tenantId, Long userId);

    boolean existsByTenant_IdAndUser_IdAndActiveTrue(Long tenantId, Long userId);

    List<TenantMember> findAllByUser_IdAndActiveTrue(Long userId);

    @Query("""
    select tm
    from TenantMember tm
    join fetch tm.tenant t
    where tm.user.id = :userId
      and tm.active = true
    """)
    List<TenantMember> findAllActiveWithTenant(@Param("userId") Long userId);

}
