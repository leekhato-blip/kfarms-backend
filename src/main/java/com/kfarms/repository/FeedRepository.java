package com.kfarms.repository;

import com.kfarms.entity.Feed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FeedRepository extends JpaRepository<Feed, Long>, JpaSpecificationExecutor<Feed> {

    @Query("SELECT DATE(f.date), SUM(f.quantityUsed) FROM Feed f WHERE f.date BETWEEN :startDate AND :endDate GROUP BY DATE(f.date)")
    List<Object[]> findDailyFeedUsageBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
