package com.kfarms.repository;

import com.kfarms.entity.FishPond;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FishPondRepository extends JpaRepository<FishPond, Long>, JpaSpecificationExecutor<FishPond> {

    @Query("SELECT f FROM FishPond f WHERE f.dateStocked BETWEEN :startDate AND :endDate")
    List<FishPond> findByDateStockedBetween(@Param("startDate") LocalDate start, @Param("endDate") LocalDate end);


    @Query("SELECT COALESCE(SUM(f.currentStock), 0) FROM FishPond f")
    long countTotalFishStock();

    List<FishPond> findAllByDateStockedBetween(LocalDate start, LocalDate end);

    @Modifying
    @Transactional
    @Query("""
    DELETE FROM FishPond f
    WHERE f.deleted = true
      AND f.deletedAt <= :threshold
""")
    int cleanupOldSoftDeleted(@Param("threshold") LocalDateTime threshold);

    @Query("select p from FishPond p " +
            "where lower(p.pondName) like lower(concat('%', :q, '%'))")
    List<FishPond> searchByName(@Param("q") String q, Pageable pageable);

    // Hatch records by pond type
    @Query("""
    select p.pondType, count(h.id)
    from FishHatch h
    join h.pond p
    where h.deleted = false
      and p.deleted = false
    group by p.pondType
    """)
    List<Object[]> countHatchesByPondType();


    // Monthly stock trend (sum of currentStock by dateStocked month)
    @Query("""
    select year(f.dateStocked), month(f.dateStocked), sum(coalesce(f.currentStock, 0))
    from FishPond f
    where (f.deleted = false or f.deleted is null)
      and f.dateStocked between :start and :end
    group by year(f.dateStocked), month(f.dateStocked)
    order by year(f.dateStocked), month(f.dateStocked)
    """)
    List<Object[]> sumMonthlyStockTotals(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );


    // Monthly hatch trend (quantity hatched)
    // Uses YEAR() + MONTH() which works well on MySQL/Hibernate.
    @Query("""
    select year(h.hatchDate), month(h.hatchDate), sum(h.quantityHatched)
    from FishHatch h
    where (h.deleted = false or h.deleted is null)
    and h.hatchDate between :start and :end
    group by year(h.hatchDate), month(h.hatchDate)
    order by year(h.hatchDate), month(h.hatchDate)
    """)
    List<Object[]> sumMonthlyHatchTotals(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

}
