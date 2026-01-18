package com.kfarms.repository;

import com.kfarms.entity.FishPond;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FishPondRepository extends JpaRepository<FishPond, Long>, JpaSpecificationExecutor<FishPond> {

    @Query("SELECT f FROM FishPond f WHERE f.dateStocked BETWEEN :startDate AND :endDate")
    List<FishPond> findByDateStockedBetween(@Param("startDate") LocalDate start, @Param("endDate") LocalDate end);


    @Query("SELECT COALESCE(SUM(f.currentStock), 0) FROM FishPond f")
    long countTotalFishStock();

    List<FishPond> findAllByDateStockedBetween(LocalDate start, LocalDate end);
}
