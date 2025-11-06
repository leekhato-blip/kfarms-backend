package com.kfarms.repository;

import com.kfarms.entity.EggProduction;
import com.kfarms.entity.Feed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EggProductionRepo extends JpaRepository<EggProduction, Long>, JpaSpecificationExecutor<EggProduction> {

    @Query("SELECT e FROM EggProduction e WHERE e.collectionDate BETWEEN :start AND :end ORDER BY e.collectionDate ASC")
    List<EggProduction> findByCollectionDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(e.goodEggs), 0L) FROM EggProduction e WHERE e.collectionDate BETWEEN :start AND :end")
    Long sumEggsBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT e.collectionDate, SUM(e.cratesProduced) FROM EggProduction e WHERE e.collectionDate BETWEEN :start AND :end GROUP BY e.collectionDate ORDER BY e.collectionDate ASC")
    List<Object[]> findDailyEggsBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    List<EggProduction> findAllByCollectionDateBetween(LocalDate start, LocalDate end);
}


