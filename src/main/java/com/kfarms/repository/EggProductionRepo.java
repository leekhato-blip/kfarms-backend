package com.kfarms.repository;

import com.kfarms.entity.EggProduction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EggProductionRepo extends JpaRepository<EggProduction, Long>, JpaSpecificationExecutor<EggProduction> {

    Optional<EggProduction> findByIdAndTenant_Id(Long id, Long tenantId);

    @Query("""
        select e from EggProduction e
        join fetch e.livestock l
        left join e.tenant t
        where e.id = :id
          and (t.id = :tenantId or (t is null and l.tenantId = :tenantId))
    """)
    Optional<EggProduction> findVisibleByIdAndTenantId(
            @Param("id") Long id,
            @Param("tenantId") Long tenantId
    );

    @Query("""
        select e from EggProduction e
        left join fetch e.livestock l
        left join e.tenant t
        where (t.id = :tenantId or (t is null and l.tenantId = :tenantId))
          and e.deleted = false
        order by e.collectionDate desc, e.id desc
    """)
    List<EggProduction> findAllActiveVisibleToTenant(@Param("tenantId") Long tenantId);

    @Query("""
        select e from EggProduction e
        left join fetch e.livestock l
        left join e.tenant t
        where (t.id = :tenantId or (t is null and l.tenantId = :tenantId))
          and e.deleted = false
          and (:start is null or e.collectionDate >= :start)
          and (:end is null or e.collectionDate <= :end)
        order by e.collectionDate asc, e.id asc
    """)
    List<EggProduction> findForExport(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Modifying
    @Query("""
        delete from EggProduction e
        where e.id = :id
          and e.tenant.id = :tenantId
    """)
    int hardDeleteByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("""
    SELECT e FROM EggProduction e
    LEFT JOIN e.tenant t
    LEFT JOIN e.livestock l
    WHERE (t.id = :tenantId OR (t IS NULL AND l.tenantId = :tenantId))
      AND e.deleted = false
      AND e.collectionDate BETWEEN :start AND :end
    ORDER BY e.collectionDate ASC
    """)
    List<EggProduction> findByCollectionDateBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
    SELECT COALESCE(SUM(e.goodEggs), 0L) FROM EggProduction e
    LEFT JOIN e.tenant t
    LEFT JOIN e.livestock l
    WHERE (t.id = :tenantId OR (t IS NULL AND l.tenantId = :tenantId))
      AND e.deleted = false
      AND e.collectionDate BETWEEN :start AND :end
    """)
    Long sumEggsBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
    SELECT e.collectionDate, SUM(e.cratesProduced) FROM EggProduction e
    LEFT JOIN e.tenant t
    LEFT JOIN e.livestock l
    WHERE (t.id = :tenantId OR (t IS NULL AND l.tenantId = :tenantId))
      AND e.deleted = false
      AND e.collectionDate BETWEEN :start AND :end
    GROUP BY e.collectionDate
    ORDER BY e.collectionDate ASC
    """)
    List<Object[]> findDailyEggsBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    List<EggProduction> findAllByCollectionDateBetween(LocalDate start, LocalDate end);

    @Query("""
    select e from EggProduction e
    join e.livestock l
    where l.tenantId = :tenantId
      and e.deleted = false
      and (
           lower(l.batchName) like lower(concat('%', :q, '%'))
        or (e.note is not null and lower(e.note) like lower(concat('%', :q, '%')))
      )
    order by e.createdAt desc
    """)
    List<EggProduction> searchByLivestockOrNote(
            @Param("tenantId") Long tenantId,
            @Param("q") String q,
            Pageable pageable
    );

//    long countByTenantId(Long tenantId);

}
