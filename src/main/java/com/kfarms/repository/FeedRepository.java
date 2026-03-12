package com.kfarms.repository;

import com.kfarms.entity.Feed;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeedRepository extends JpaRepository<Feed, Long>, JpaSpecificationExecutor<Feed> {

    @Query("""
    SELECT DATE(f.date), SUM(f.quantityUsed) FROM Feed f
    WHERE f.tenant.id = :tenantId
      AND (f.deleted = false OR f.deleted is null)
      AND f.date BETWEEN :start AND :end
    GROUP BY DATE(f.date)
    """)
    List<Object[]> findDailyFeedUsageBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
    SELECT COALESCE(SUM(f.quantityUsed), 0) FROM Feed f
    WHERE f.tenant.id = :tenantId
      AND (f.deleted = false OR f.deleted is null)
      AND f.date BETWEEN :start AND :end
    """)
    Double sumFeedUsedBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
    select f from Feed f
    where f.tenant.id = :tenantId
      and (f.deleted = false or f.deleted is null)
      and f.date between :start and :end
    order by f.date asc
    """)
    List<Feed> findAllByDateBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    List<Feed> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
    select f from Feed f
    where f.tenant.id = :tenantId
      and (f.deleted = false or f.deleted is null)
      and lower(f.feedName) like lower(concat('%', :q, '%'))
    order by f.createdAt desc
    """)
    List<Feed> searchByName(@Param("tenantId") Long tenantId, @Param("q") String q, Pageable pageable);

    long countByTenantId(Long tenantId);

    Optional<Feed> findByIdAndTenant_Id(Long id, Long tenantId);

    @Query("""
    select f from Feed f
    where f.tenant.id = :tenantId
      and (f.deleted = false or f.deleted is null)
    order by f.date desc, f.id desc
    """)
    List<Feed> findAllActiveByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query("""
    delete from Feed f
    where f.id = :id
      and f.tenant.id = :tenantId
    """)
    int hardDeleteByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("""
    select f from Feed f
    where f.tenant.id = :tenantId
      and (f.deleted = false or f.deleted is null)
    order by f.date desc, f.id desc
    """)
    List<Feed> findRecentActiveByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("""
    select f from Feed f
    where f.tenant.id = :tenantId
      and (f.deleted = false or f.deleted is null)
      and f.date between :start and :end
    order by f.date desc, f.id desc
    """)
    List<Feed> findActiveByTenantIdAndDateBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

}
