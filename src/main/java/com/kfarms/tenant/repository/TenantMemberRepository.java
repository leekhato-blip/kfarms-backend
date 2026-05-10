package com.kfarms.tenant.repository;

import com.kfarms.tenant.entity.TenantRole;
import com.kfarms.tenant.entity.TenantMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TenantMemberRepository extends JpaRepository<TenantMember, Long> {

    Optional<TenantMember> findByTenant_IdAndUser_IdAndActiveTrue(Long tenantId, Long userId);

    long countByUser_IdAndRoleAndActiveTrue(Long userId, TenantRole role);

    boolean existsByTenant_IdAndUser_Id(Long tenantId, Long userId);

    @Query("""
    select tm from TenantMember tm
    join fetch tm.user u
    where tm.tenant.id = :tenantId
    """)
    List<TenantMember> findAllByTenantIdWithUser(@Param("tenantId") Long tenantId);

    boolean existsByTenant_IdAndUser_IdAndActiveTrue(Long tenantId, Long userId);

    List<TenantMember> findAllByUser_IdAndActiveTrue(Long userId);

    Optional<TenantMember> findByIdAndTenant_Id(Long id, Long tenantId);
    Optional<TenantMember> findByTenant_IdAndUser_Id(Long tenantId, Long userId);

    @Query("""
    select tm from TenantMember tm
    join fetch tm.user u
    where tm.tenant.id = :tenantId
      and tm.active = true
      and tm.role in :roles
    """)
    List<TenantMember> findActiveMembersByTenantAndRoles(
            @Param("tenantId") Long tenantId,
            @Param("roles") Collection<TenantRole> roles
    );

    int countByTenant_Id(Long tenantId);

    int countByTenant_IdAndActiveTrue(Long tenantId);

    List<TenantMember> findByTenant_Id(Long tenantId);

    Optional<TenantMember> findFirstByTenantIdAndRoleOrderByIdAsc(Long tenantId, TenantRole role);

    List<TenantMember> findByTenantIdAndRoleInAndActiveTrue(Long tenantId, Collection<TenantRole> roles);


    @Query("""
    select tm
    from TenantMember tm
    join fetch tm.tenant t
    where tm.user.id = :userId
      and tm.active = true
    """)
    List<TenantMember> findAllActiveWithTenant(@Param("userId") Long userId);

    @Query("""
    select tm from TenantMember tm
    join fetch tm.user u
    where tm.tenant.id = :tenantId
      and tm.role = :role
    order by tm.id asc
    """)
    List<TenantMember> findFirstByTenantIdAndRoleWithUser(
            @Param("tenantId") Long tenantId,
            @Param("role") TenantRole role
    );


    int countByUser_Id(Long userId);



}
