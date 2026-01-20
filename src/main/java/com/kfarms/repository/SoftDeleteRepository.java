package com.kfarms.repository;

import com.kfarms.entity.SoftDeletable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@NoRepositoryBean
public interface SoftDeleteRepository<T, ID>
        extends JpaRepository<T, ID> {

    @SuppressWarnings("JpaQueryReferencesInspection")
    @Modifying
    @Transactional
    @Query("DELETE FROM #{#entityName} e WHERE e.deleted = true AND e.deletedAt <= :threshold")
    int deleteSoftDeletedOlderThan(@Param("threshold") LocalDateTime threshold);

}


