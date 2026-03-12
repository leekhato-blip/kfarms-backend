package com.kfarms.controller;


import com.kfarms.entity.ApiResponse;
import com.kfarms.health.dto.HealthEventDto;
import com.kfarms.health.entity.HealthEvent;
import com.kfarms.health.mapper.HealthEventMapper;
import com.kfarms.health.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    // Trigger a rule manually
    @PostMapping("/rules/{ruleCode}/trigger")
    public ResponseEntity<ApiResponse<HealthEventDto>> triggerRule(
            @PathVariable String ruleCode,
            @RequestParam(required = false) String contextNote,
            @RequestParam(defaultValue = "UNKNOWN") String season,
            @RequestParam(required = false) String sourceKey
    ) {
        HealthEvent event = healthService.triggerRuleByCode(
                ruleCode,
                contextNote,
                season,
                sourceKey
        );

        if (event == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse<>(false, "Rule cooldown active", null));
        }

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Rule Triggered", HealthEventMapper.toDto(event))
        );
    }

    // Get all active events
    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<HealthEventDto>>> getAllEvents() {
        List<HealthEventDto> events = healthService.getActiveEvents().stream()
                .sorted((a, b) -> b.getTriggeredAt().compareTo(a.getTriggeredAt()))
                .map(HealthEventMapper::toDto)
                .toList();
        
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Events fetched", events)
        );
    }

    // Acknowledge an event
    @PutMapping("/events/{id}/acknowledge")
    public ResponseEntity<ApiResponse<HealthEventDto>> acknowledge(@PathVariable Long id) {
        HealthEvent acknowledgedEvent = healthService.acknowledgeEvent(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Event Acknowledged", HealthEventMapper.toDto(acknowledgedEvent))
        );
    }

    // Handle an event
    @PostMapping("/events/{id}/handle")
    public ResponseEntity<ApiResponse<HealthEventDto>> handle(@PathVariable Long id) {
        HealthEvent handledEvent = healthService.handleEvent(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Event handled", HealthEventMapper.toDto(handledEvent))
        );
    }
}
