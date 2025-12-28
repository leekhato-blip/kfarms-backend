package com.kfarms.health.service;


import com.kfarms.health.entity.HealthRule;
import com.kfarms.health.enums.HealthRuleCategory;
import com.kfarms.health.enums.HealthSeverity;
import com.kfarms.health.repo.HealthRuleRepo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HealthRuleSeeder {

    private final HealthRuleRepo ruleRepo;

    @PostConstruct
    public void seed() {
        if (ruleRepo.count() > 0) return;

        ruleRepo.saveAll(List.of(

                // ================= WEATHER =================
                rule("HEAT_STRESS_POULTRY",
                        "Heat Stress Warning",
                        HealthRuleCategory.WEATHER,
                        HealthSeverity.WARNING,
                        "High temperature may stress poultry and reduce feed intake",
                        2),

                rule("EXTREME_HEAT_POULTRY",
                        "Extreme Heat Alert",
                        HealthRuleCategory.WEATHER,
                        HealthSeverity.CRITICAL,
                        "Extreme heat can cause sudden poultry deaths",
                        4),

                rule("COLD_STRESS_POULTRY",
                        "Cold Stress Warning",
                        HealthRuleCategory.WEATHER,
                        HealthSeverity.WARNING,
                        "Low temperature weakens poultry immunity",
                        6),

                rule("HIGH_HUMIDITY_POULTRY",
                        "High Humidity Risk",
                        HealthRuleCategory.WEATHER,
                        HealthSeverity.WARNING,
                        "High humidity increases disease risk and poor ventilation",
                        3),

                // ================= WATER / FISH =================
                rule("LOW_OXYGEN_FISH",
                        "Low Oxygen Alert",
                        HealthRuleCategory.WATER,
                        HealthSeverity.CRITICAL,
                        "Low oxygen levels may suffocate fish",
                        1),

                rule("HIGH_WATER_TEMP_FISH",
                        "High Water Temperature",
                        HealthRuleCategory.WATER,
                        HealthSeverity.WARNING,
                        "Warm water reduces oxygen availability for fish",
                        2),

                rule("DIRTY_WATER_FISH",
                        "Poor Water Quality",
                        HealthRuleCategory.WATER,
                        HealthSeverity.WARNING,
                        "Dirty water can cause fish disease and slow growth",
                        4),

                // ================= POULTRY HEALTH =================

                rule("UNUSUAL_MORTALITY_POULTRY",
                        "Unusual Mortality Alert",
                        HealthRuleCategory.HEALTH,
                        HealthSeverity.CRITICAL,
                        "Sudden deaths detected in poultry stock",
                        12),

                rule("UNUSUAL_MORTALITY_FISH",
                        "Unusual Fish Mortality Alert",
                        HealthRuleCategory.DISEASE,
                        HealthSeverity.CRITICAL,
                        "Sudden or abnormal fish deaths detected in pond",
                        6)


        ));


        System.out.println("Health rules seeded");
    }

    private HealthRule rule(String code, String title,
                            HealthRuleCategory category,
                            HealthSeverity severity,
                            String desc,
                            int cooldown) {

        HealthRule r = new HealthRule();
        r.setCode(code);
        r.setTitle(title);
        r.setCategory(category);
        r.setSeverity(severity);
        r.setDescription(desc);
        r.setCooldownHours(cooldown);
        r.setActive(true);
        return r;
    }
}
