package com.kfarms.repository;

import com.kfarms.entity.SupportAssistantMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportAssistantMessageRepository extends JpaRepository<SupportAssistantMessage, Long> {

    List<SupportAssistantMessage> findByTenant_IdAndDeletedFalseOrderByCreatedAtAscIdAsc(Long tenantId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from SupportAssistantMessage message where message.tenant.id = :tenantId")
    void deleteAllByTenantId(@Param("tenantId") Long tenantId);
}
