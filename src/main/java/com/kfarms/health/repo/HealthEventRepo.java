package com.kfarms.health.repo;

import com.kfarms.health.entity.HealthEvent;
import com.kfarms.health.entity.HealthRule;
import com.kfarms.health.enums.HealthEventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HealthEventRepo extends JpaRepository<HealthEvent, Long> {

    boolean existsByTenant_IdAndRuleAndStatusInAndSourceKeyIgnoreCase(
            Long tenantId,
            HealthRule rule,
            Collection<HealthEventStatus> statuses,
            String sourceKey
    );

    boolean existsByTenant_IdAndRuleAndSourceKeyIgnoreCaseAndTriggeredAtAfter(
            Long tenantId,
            HealthRule rule,
            String sourceKey,
            LocalDateTime after
    );

    List<HealthEvent> findByTenant_IdAndStatusInOrderByTriggeredAtDesc(
            Long tenantId,
            Collection<HealthEventStatus> statuses
    );

    Optional<HealthEvent> findByIdAndTenant_Id(Long id, Long tenantId);

    @Query("""
    select e from HealthEvent e
    where e.tenant.id = :tenantId
    and (
        lower(coalesce(e.sourceKey, '')) like lower(concat('%', :q, '%'))
        or lower(coalesce(e.contextNote, '')) like lower(concat('%', :q, '%'))
        or lower(e.rule.title) like lower(concat('%', :q, '%'))
    )
    order by e.triggeredAt desc
    """)
    List<HealthEvent> search(@Param("tenantId") Long tenantId, @Param("q") String q, Pageable pageable);

}
