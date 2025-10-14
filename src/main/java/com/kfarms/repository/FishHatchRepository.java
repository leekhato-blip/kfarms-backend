package com.kfarms.repository;

import com.kfarms.entity.FishHatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FishHatchRepository extends JpaRepository<FishHatch, Long> {

    // 🐟 Filter by pond
    List<FishHatch> findByPondId(Long pondId);

    // 🧮 For summary — count hatch records for a given pond
    @Query("SELECT COUNT(fh) FROM FishHatch fh WHERE fh.pond.id = :pondId")
    long countByPond(Long pondId);

    // 🗓️ Optional: find hatches between two dates (for future analytics)
    List<FishHatch> findByHatchDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT fh.pond.id, COUNT(fh) FROM FishHatch fh WHERE fh.deleted = false GROUP BY fh.pond.id")
    List<Object[]> countHatchesGroupedByPond();

}
