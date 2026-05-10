package com.kfarms.repository;

import com.kfarms.entity.Sales;
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
public interface SalesRepository
        extends JpaRepository<Sales, Long>,
        JpaSpecificationExecutor<Sales> {


    @Query("""
    SELECT s.salesDate, SUM(s.totalPrice)
    FROM Sales s
    WHERE s.tenant.id = :tenantId
      AND (s.deleted = false OR s.deleted IS NULL)
      AND s.salesDate BETWEEN :start AND :end
    GROUP BY s.salesDate
    ORDER BY s.salesDate ASC
    """)
    List<Object[]> findDailySalesBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
    SELECT s FROM Sales s
    WHERE s.tenant.id = :tenantId
      AND (s.deleted = false OR s.deleted IS NULL)
      AND s.salesDate BETWEEN :start AND :end
    ORDER BY s.salesDate ASC
    """)
    List<Sales> findBySalesDateBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
    SELECT COALESCE(SUM(s.totalPrice), 0)
    FROM Sales s
    WHERE s.tenant.id = :tenantId
      AND (s.deleted = false OR s.deleted IS NULL)
      AND s.salesDate BETWEEN :start AND :end
    """)
    BigDecimal sumTotalBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
    SELECT COALESCE(SUM(s.unitPrice * s.quantity), 0)
    FROM Sales s
    WHERE s.tenant.id = :tenantId
      AND (s.deleted = false OR s.deleted IS NULL)
      AND s.salesDate BETWEEN :start AND :end
    """)
    BigDecimal sumExpensesBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
    SELECT COALESCE(SUM(s.totalPrice), 0)
    FROM Sales s
    WHERE (s.deleted = false OR s.deleted IS NULL)
    """)
    BigDecimal sumAllActiveRevenue();

    @Query(
            value = """
            select to_char(s.sales_date, 'YYYY-MM') as month, sum(s.total_price)
            from sales s
            where s.tenant_id = :tenantId
              and coalesce(s.deleted, false) = false
              and cast(extract(year from s.sales_date) as integer) = :year
            group by to_char(s.sales_date, 'YYYY-MM')
            order by to_char(s.sales_date, 'YYYY-MM')
            """,
            nativeQuery = true
    )
    List<Object[]> getMonthlyRevenue(@Param("tenantId") Long tenantId, @Param("year") int year);

    @Query("""
    select s from Sales s
    where s.tenant.id = :tenantId
      and (s.deleted = false or s.deleted is null)
    order by s.createdAt desc
    """)
    List<Sales> findAllByOrderByCreatedAtDesc(@Param("tenantId") Long tenantId, Pageable pageable);

    @Modifying
    @Transactional
    @Query("""
    DELETE FROM Sales s
    WHERE s.deleted = true
      AND s.deletedAt <= :threshold
""")
    int cleanupOldSoftDeleted(@Param("threshold") LocalDateTime threshold);


    @Query("""
    select s from Sales s
    where s.tenant.id = :tenantId
      and (s.deleted = false or s.deleted is null)
      and lower(s.itemName) like lower(concat('%', :q, '%'))
    """)
    List<Sales> searchByItemName(@Param("tenantId") Long tenantId, @Param("q") String q, Pageable pageable);

    long countByTenantId(Long tenantId);

    Optional<Sales> findByIdAndTenant_Id(Long id, Long tenantId);

    @Query("""
    select s from Sales s
    where s.tenant.id = :tenantId
      and (s.deleted = false or s.deleted is null)
    order by s.salesDate desc, s.id desc
    """)
    List<Sales> findAllActiveByTenantId(@Param("tenantId") Long tenantId);

    @Query("""
    select s from Sales s
    where s.tenant.id = :tenantId
      and (s.deleted = false or s.deleted is null)
    order by s.salesDate desc, s.id desc
    """)
    List<Sales> findRecentActiveByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("""
    select s from Sales s
    where s.tenant.id = :tenantId
      and (s.deleted = false or s.deleted is null)
      and s.salesDate between :start and :end
    order by s.salesDate desc, s.id desc
    """)
    List<Sales> findActiveByTenantIdAndSalesDateBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );



}
