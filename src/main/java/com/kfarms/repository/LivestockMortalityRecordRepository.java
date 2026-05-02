package com.kfarms.repository;

import com.kfarms.entity.LivestockMortalityRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LivestockMortalityRecordRepository extends JpaRepository<LivestockMortalityRecord, Long> {
    List<LivestockMortalityRecord> findAllByTenantIdAndDeletedFalse(Long tenantId);
}
