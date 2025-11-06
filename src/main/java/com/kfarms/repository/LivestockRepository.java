package com.kfarms.repository;

import com.kfarms.entity.Feed;
import com.kfarms.entity.Livestock;
import com.kfarms.entity.LivestockType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
@Repository
public interface LivestockRepository extends JpaRepository<Livestock, Long>, JpaSpecificationExecutor<Livestock> {

    @Query("SELECT l FROM Livestock l WHERE l.deleted = false ORDER BY l.arrivalDate ASC")
    List<Livestock> findAllActive();

    @Query("SELECT l FROM Livestock l WHERE l.type = :type AND l.deleted = false ORDER BY l.arrivalDate ASC")
    List<Livestock> findByType(@Param("type") LivestockType type);

    @Query("SELECT COALESCE(SUM(l.currentStock), 0) FROM Livestock l WHERE l.deleted = false")
    long countAllActiveLivestock();

    @Query("SELECT l FROM Livestock l WHERE l.arrivalDate BETWEEN :startDate AND :endDate")
    List<Livestock> findByArrivalDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<Livestock> findAllByArrivalDateBetween(LocalDate start, LocalDate end);
}

