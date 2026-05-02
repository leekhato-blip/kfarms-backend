package com.kfarms.repository;

import com.kfarms.entity.FishPondMortalityRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FishPondMortalityRecordRepository extends JpaRepository<FishPondMortalityRecord, Long> {
    @Query("""
    select r from FishPondMortalityRecord r
    where r.pond.tenant.id = :tenantId
      and (r.deleted = false or r.deleted is null)
    order by r.mortalityDate desc, r.id desc
    """)
    List<FishPondMortalityRecord> findAllActiveByTenantId(@Param("tenantId") Long tenantId);
}
