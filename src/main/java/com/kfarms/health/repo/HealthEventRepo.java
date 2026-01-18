package com.kfarms.health.repo;

import com.kfarms.health.entity.HealthEvent;
import com.kfarms.health.entity.HealthRule;
import com.kfarms.health.enums.HealthEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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


}
