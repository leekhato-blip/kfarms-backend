package com.kfarms.health.entity;


import com.kfarms.health.enums.HealthRuleCategory;
import com.kfarms.health.enums.HealthSeverity;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "health_rules")
@Data
public class HealthRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code; // e.g. HEAT_STRESS_LAYERS_FISH

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    private HealthRuleCategory category;


    @Enumerated(EnumType.STRING)
    private HealthSeverity severity;

    @Column(length = 1000)
    private String description;

    @Column(name = "cooldown_hours")
    private Integer cooldownHours;

    private Boolean active = true;

}
