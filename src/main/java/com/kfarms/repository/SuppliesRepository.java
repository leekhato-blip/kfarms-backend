package com.kfarms.repository;

import com.kfarms.entity.Supplies;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
@Repository
public interface SuppliesRepository extends JpaRepository<Supplies, Long>, JpaSpecificationExecutor<Supplies> {

    @Query("SELECT s.supplyDate, SUM(s.totalPrice) FROM Supplies s " +
            "WHERE s.supplyDate BETWEEN :start AND :end GROUP BY s.supplyDate ORDER BY s.supplyDate")
    List<Object[]> findDailySuppliesBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(s.unitPrice * s.quantity), 0) FROM Supplies s WHERE s.supplyDate BETWEEN :start AND :end")
    BigDecimal sumTotalBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    List<Supplies> findAllBySupplyDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(SUM(s.totalPrice), 0) FROM Supplies s WHERE s.supplyDate BETWEEN :start AND :end")
    BigDecimal sumSupplyCostBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT TO_CHAR(sp.supplyDate, 'YYYY-MM') AS month, SUM(sp.totalPrice) " +
            "FROM Supplies sp WHERE sp.deleted = false GROUP BY month ORDER BY month")
    List<Object[]> getMonthlyExpenses();
}
