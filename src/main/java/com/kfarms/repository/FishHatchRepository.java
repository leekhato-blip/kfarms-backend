package com.kfarms.repository;

import com.kfarms.entity.FishHatch;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FishHatchRepository extends JpaRepository<FishHatch, Long> {

    // 🐟 Filter by pond
    List<FishHatch> findByPondId(Long pondId);
    List<FishHatch> findAllByTenant_IdAndDeletedFalse(Long tenantId);
    List<FishHatch> findAllByTenant_IdAndDeletedTrue(Long tenantId);
    Optional<FishHatch> findByIdAndTenant_Id(Long id, Long tenantId);

    // 🧮 For summary — count hatch records for a given pond
    @Query("SELECT COUNT(fh) FROM FishHatch fh WHERE fh.pond.id = :pondId")
    long countByPond(Long pondId);

    // 🗓️ Optional: find hatches between two dates (for future analytics)
    List<FishHatch> findByHatchDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT fh.pond.id, COUNT(fh) FROM FishHatch fh WHERE fh.deleted = false GROUP BY fh.pond.id")
    List<Object[]> countHatchesGroupedByPond();

    @Query("""
    select h from FishHatch h
    join h.pond p
    where h.tenant.id = :tenantId
      and h.deleted = false
      and (
           lower(p.pondName) like lower(concat('%', :q, '%'))
        or (h.note is not null and lower(h.note) like lower(concat('%', :q, '%')))
      )
    order by h.createdAt desc
    """)
    List<FishHatch> searchByName(@Param("tenantId") Long tenantId, @Param("q") String q, Pageable pageable);

    @Query("""
    select p.pondType, count(h.id)
    from FishHatch h
    join h.pond p
    where (h.deleted = false or h.deleted is null)
    and (p.deleted = false or p.deleted is null)
    group by p.pondType
    """)
    List<Object[]> countHatchesByPondType();


    @Query("""
    select year(h.hatchDate), month(h.hatchDate), sum(h.quantityHatched)
    from FishHatch h
    where (h.deleted = false or h.deleted is null)
    and h.hatchDate between :start and :end
    group by year(h.hatchDate), month(h.hatchDate)
    order by year(h.hatchDate), month(h.hatchDate)
    """)
    List<Object[]> sumMonthlyHatchTotals(@Param("start") LocalDate start,
                                         @Param("end") LocalDate end);

    @Modifying
    @Query("""
    delete from FishHatch h
    where h.id = :id
      and h.tenant.id = :tenantId
    """)
    int hardDeleteByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);


}
