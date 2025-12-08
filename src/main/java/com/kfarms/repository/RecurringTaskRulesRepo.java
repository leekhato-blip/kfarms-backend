package com.kfarms.repository;

import com.kfarms.entity.RecurringTaskRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecurringTaskRulesRepo extends JpaRepository<RecurringTaskRule, Long> {

    // fetch only enabled rules
    List<RecurringTaskRule> findByEnabledTrue();
}
