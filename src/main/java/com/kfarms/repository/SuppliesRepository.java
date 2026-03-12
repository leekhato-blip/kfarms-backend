package com.kfarms.repository;

import com.kfarms.entity.Supplies;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Repository
public interface SuppliesRepository extends JpaRepository<Supplies, Long>, JpaSpecificationExecutor<Supplies> {

    @Query("""
    SELECT s.supplyDate, SUM(s.totalPrice)
    FROM Supplies s
    WHERE s.tenant.id = :tenantId
      AND (s.deleted = false OR s.deleted IS NULL)
      AND s.supplyDate BETWEEN :start AND :end
    GROUP BY s.supplyDate
    ORDER BY s.supplyDate
    """)
    List<Object[]> findDailySuppliesBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
    SELECT COALESCE(SUM(s.unitPrice * s.quantity), 0)
    FROM Supplies s
    WHERE s.tenant.id = :tenantId
      AND (s.deleted = false OR s.deleted IS NULL)
      AND s.supplyDate BETWEEN :start AND :end
    """)
    BigDecimal sumTotalBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
    SELECT s FROM Supplies s
    WHERE s.tenant.id = :tenantId
      AND (s.deleted = false OR s.deleted IS NULL)
      AND s.supplyDate BETWEEN :start AND :end
    ORDER BY s.supplyDate ASC
    """)
    List<Supplies> findAllBySupplyDateBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
    SELECT COALESCE(SUM(s.totalPrice), 0)
    FROM Supplies s
    WHERE s.tenant.id = :tenantId
      AND (s.deleted = false OR s.deleted IS NULL)
      AND s.supplyDate BETWEEN :start AND :end
    """)
    BigDecimal sumSupplyCostBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query(
            value = """
            select to_char(sp.supply_date, 'YYYY-MM') as month, sum(sp.total_price)
            from supplies sp
            where sp.tenant_id = :tenantId
              and coalesce(sp.deleted, false) = false
              and cast(extract(year from sp.supply_date) as integer) = :year
            group by to_char(sp.supply_date, 'YYYY-MM')
            order by to_char(sp.supply_date, 'YYYY-MM')
            """,
            nativeQuery = true
    )
    List<Object[]> getMonthlyExpenses(@Param("tenantId") Long tenantId, @Param("year") int year);

    @Query("""
    select s from Supplies s
    where s.tenant.id = :tenantId
      and (s.deleted = false or s.deleted is null)
    order by s.createdAt desc
    """)
    List<Supplies> findAllByOrderByCreatedAtDesc(@Param("tenantId") Long tenantId, Pageable pageable);

    @Modifying
    @Transactional
    @Query("""
    DELETE FROM Supplies s
    WHERE s.deleted = true
      AND s.deletedAt <= :threshold
""")
    int cleanupOldSoftDeleted(@Param("threshold") LocalDateTime threshold);

    @Query("""
    select s from Supplies s
    where s.tenant.id = :tenantId
      and s.deleted = false
      and lower(s.itemName) like lower(concat('%', :q, '%'))
    order by s.createdAt desc
    """)
    List<Supplies> searchByItemName(@Param("tenantId") Long tenantId, @Param("q") String q, Pageable pageable);

    Optional<Supplies> findByIdAndTenant_Id(Long id, Long tenantId);

    @Query("""
    select s from Supplies s
    where s.tenant.id = :tenantId
      and (s.deleted = false or s.deleted is null)
    order by s.supplyDate desc, s.id desc
    """)
    List<Supplies> findAllActiveByTenantId(@Param("tenantId") Long tenantId);

}
