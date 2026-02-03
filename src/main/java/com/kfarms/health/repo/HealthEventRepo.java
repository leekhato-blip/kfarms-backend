package com.kfarms.health.repo;

import com.kfarms.health.entity.HealthEvent;
import com.kfarms.health.entity.HealthRule;
import com.kfarms.health.enums.HealthEventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface HealthEventRepo extends JpaRepository<HealthEvent, Long> {

    boolean existsByRuleAndStatusAndSourceKey(
            HealthRule rule,
            HealthEventStatus status,
            String sourceKey
    );

    boolean existsByRuleAndTriggeredAtAfter(
            HealthRule rule,
            LocalDateTime after
    );

    List<HealthEvent> findByRuleAndStatus(
            HealthRule rule,
            HealthEventStatus status
    );

    boolean existsByRuleAndStatus(
            HealthRule rule, HealthEventStatus status
    );

    @Query("""
    select e from HealthEvent e
    where lower(e.sourceKey) like lower(concat('%', :q, '%'))
    or lower(e.contextNote) like lower(concat('%', :q, '%'))
    order by e.triggeredAt desc
    """)
    List<HealthEvent> search(@Param("q") String q, Pageable pageable);

}
