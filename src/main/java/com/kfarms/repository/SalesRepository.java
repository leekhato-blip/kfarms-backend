package com.kfarms.repository;

import com.kfarms.entity.Sales;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SalesRepository extends JpaRepository<Sales, Long>, JpaSpecificationExecutor<Sales> {

    @Query("SELECT DATE(s.salesDate), SUM(s.totalPrice) FROM Sales s WHERE s.salesDate BETWEEN :startDate AND :endDate GROUP BY DATE(s.salesDate)")
    List<Object[]> findDailySalesBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT s FROM Sales s WHERE s.salesDate BETWEEN :start AND :end ORDER BY s.salesDate ASC")
    List<Sales> findBySalesDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(s.totalPrice),0) FROM Sales s WHERE s.salesDate BETWEEN :start AND :end")
    BigDecimal sumTotalBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(s.unitPrice * s.quantity),0) FROM Sales s WHERE s.salesDate BETWEEN :start AND :end")
    BigDecimal sumExpensesBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
