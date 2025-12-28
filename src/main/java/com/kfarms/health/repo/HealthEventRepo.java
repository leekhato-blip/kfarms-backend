package com.kfarms.health.repo;

import com.kfarms.health.entity.HealthEvent;
import com.kfarms.health.entity.HealthRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface HealthEventRepo extends JpaRepository<HealthEvent, Long> {
    boolean existsByRuleAndTriggeredAtAfter(HealthRule rule, LocalDateTime time);

}
