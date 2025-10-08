package com.kfarms.repository;

import com.kfarms.entity.Livestock;
import com.kfarms.entity.LivestockType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface LivestockRepository extends JpaRepository<Livestock, Long>, JpaSpecificationExecutor<Livestock> {

//    @Query("""
//    SELECT l FROM Livestock l
//    WHERE (:batchName IS NULL OR LOWER(l.batchName) LIKE LOWER(CONCAT('%', :batchName, '%')))
//      AND (:type IS NULL OR l.type = :type)
//      AND (:arrivalDate IS NULL OR l.arrivalDate = :arrivalDate)
//    """)
//    Page<Livestock> findByFilters(
//            @Param("batchName") String batchName,
//            @Param("type") LivestockType type,
//            @Param("arrivalDate")LocalDate arrivalDate,
//            Pageable pageable
//            );
}
