package com.kfarms.health.dto;

import com.kfarms.health.enums.HealthEventStatus;
import com.kfarms.health.enums.HealthRuleCategory;
import com.kfarms.health.enums.HealthSeverity;
import lombok.Data;

import java.util.List;

@Data
public class HealthEventDto {
    private Long id;

    private String title;               // Rule title
    private HealthRuleCategory category;
    private HealthSeverity severity;
    private HealthEventStatus status;

    private String triggeredAt;          // formatted
    private Integer responseMinutes;     // derived

    private List<String> adviceSteps;
    private String contextNote;
}
