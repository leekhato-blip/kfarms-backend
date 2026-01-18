package com.kfarms.health.repo;

import com.kfarms.health.entity.HealthRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HealthRuleRepo extends JpaRepository<HealthRule, Long> {

    Optional<HealthRule> findByCodeIgnoreCase(String code);
}
