package com.kfarms.health.service;

import com.kfarms.health.dto.AdviceContext;
import com.kfarms.health.entity.HealthEvent;
import com.kfarms.health.entity.HealthRule;
import com.kfarms.health.enums.HealthEventStatus;
import com.kfarms.health.repo.HealthEventRepo;
import com.kfarms.health.repo.HealthRuleRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class HealthService {

    private final HealthRuleRepo ruleRepo;
    private final HealthEventRepo eventRepo;
    private final HealthAdviceService adviceService;

    public HealthService(
            HealthRuleRepo ruleRepo,
            HealthEventRepo eventRepo,
            HealthAdviceService adviceService
    ) {
        this.ruleRepo = ruleRepo;
        this.eventRepo = eventRepo;
        this.adviceService = adviceService;
    }

    /**
     * SINGLE SOURCE OF TRUTH for triggering health rules
     */
    public HealthEvent triggerRuleByCode(
            String code,
            String contextNote,
            String season
    ) {
        System.out.println(" HealthService.triggerRuleByCode() CALLED: " + code);
        HealthRule rule = ruleRepo.findByCodeIgnoreCase(code)
                .orElseThrow(() ->
                        new RuntimeException("Rule not found " + code)
                );

        boolean activeAlertExists =
                eventRepo.existsByRuleAndStatus(rule, HealthEventStatus.NEW);

        if (activeAlertExists){
            return null;
        }

        // cooldown check
        System.out.println(">>> triggerRuleByCode() called for rule: " + code);
        if (rule.getCooldownHours() != null && rule.getCooldownHours() > 0) {
            LocalDateTime limit =
                    LocalDateTime.now().minusHours(rule.getCooldownHours());
            boolean exists =
                    eventRepo.existsByRuleAndTriggeredAtAfter(rule, limit);
            if (exists) {
                System.out.println(
                        "Cooldown active for rule " + rule.getCode()
                );

                return null;
            }
        }

        // create event
        HealthEvent event = new HealthEvent();
        event.setRule(rule);
        event.setSeverity(rule.getSeverity());
        event.setTriggeredAt(LocalDateTime.now());
        event.setContextNote(contextNote);

        // generate advice
        System.out.println(">>> HealthService: about to call adviceService.generateAdvice()");
        System.out.println(">>> adviceService.class = " + adviceService.getClass().getName());

        AdviceContext adviceContext = new AdviceContext();
                adviceContext.setRuleCode(rule.getCode());
                adviceContext.setRuleTitle(rule.getTitle());
                adviceContext.setContextNote(contextNote);
                adviceContext.setSeason(season);
                adviceContext.setLivestockType("layers, turkeys, ducks, fish");
        List<String> adviceSteps =
                adviceService.generateAdvice(adviceContext);

        // advice belongs to EVENT
        event.setAdviceSteps(adviceSteps);

        return eventRepo.save(event);
    }
}
