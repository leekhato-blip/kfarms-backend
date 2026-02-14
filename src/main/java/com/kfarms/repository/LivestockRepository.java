package com.kfarms.repository;

import com.kfarms.entity.Feed;
import com.kfarms.entity.Livestock;
import com.kfarms.entity.LivestockType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LivestockRepository extends JpaRepository<Livestock, Long>, JpaSpecificationExecutor<Livestock> {

    // Tenant-safe finders
    Optional<Livestock> findByIdAndTenantId(Long id, Long tenantId);

    // Tenant-safe JPQL queries
    @Query("SELECT l FROM Livestock l WHERE l.tenantId = :tenantId AND l.deleted = false ORDER BY l.arrivalDate ASC")
    List<Livestock> findAllActive(@Param("tenantId") Long tenantId);

    @Query("SELECT l FROM Livestock l WHERE l.tenantId = :tenantId AND l.type = :type AND l.deleted = false ORDER BY l.arrivalDate ASC")
    List<Livestock> findByType(@Param("tenantId") Long tenantId, @Param("type") LivestockType type);

    @Query("SELECT COALESCE(SUM(l.currentStock), 0) FROM Livestock l WHERE l.tenantId = :tenantId AND l.deleted = false")
    long countAllActiveLivestock(@Param("tenantId") Long tenantId);

    @Query("SELECT l FROM Livestock l WHERE l.tenantId = :tenantId AND l.arrivalDate BETWEEN :startDate AND :endDate")
    List<Livestock> findByArrivalDateBetween(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        select l from Livestock l
        where l.tenantId = :tenantId
          and l.deleted = false
          and (
              lower(l.batchName) like lower(concat('%', :q, '%'))
              or (l.note is not null and lower(l.note) like lower(concat('%', :q, '%')))
          )
        order by l.createdAt desc
    """)
    List<Livestock> searchByNameOrBatch(@Param("tenantId") Long tenantId,
                                        @Param("q") String q,
                                        Pageable pageable);
}


