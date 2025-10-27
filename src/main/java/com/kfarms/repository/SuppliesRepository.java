package com.kfarms.repository;

import com.kfarms.entity.Supplies;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SuppliesRepository extends JpaRepository<Supplies, Long>, JpaSpecificationExecutor<Supplies> {

    @Query("SELECT DATE(p.date), SUM(p.totalAmount) FROM Purchase p WHERE p.date BETWEEN :start AND :end GROUP BY DATE(p.date) ORDER BY DATE(p.date)")
    List<Object[]> findDailyPurchasesBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

}
