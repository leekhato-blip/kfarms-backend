package com.kfarms.health.mapper;

import com.kfarms.health.dto.HealthEventDto;
import com.kfarms.health.entity.HealthEvent;

import java.time.Duration;
import java.time.format.DateTimeFormatter;

public class HealthEventMapper {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static HealthEventDto toDto(HealthEvent event) {
        HealthEventDto dto = new HealthEventDto();

        dto.setId(event.getId());
        dto.setTitle(event.getRule().getTitle());
        dto.setCategory(event.getRule().getCategory());
        dto.setSeverity(event.getSeverity());
        dto.setStatus(event.getStatus());
        dto.setAdviceSteps(event.getAdviceSteps());

        dto.setContextNote(event.getContextNote());
        dto.setTriggeredAt(event.getTriggeredAt().format(FORMATTER));

        if (event.getAcknowledgedAt() != null) {
            long minutes = Duration.between(
                    event.getTriggeredAt(),
                    event.getAcknowledgedAt()
            ).toMinutes();
            dto.setResponseMinutes((int) minutes);
        }

        return dto;
    }
}
