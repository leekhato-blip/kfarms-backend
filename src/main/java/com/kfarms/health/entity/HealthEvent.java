package com.kfarms.health.entity;

import com.kfarms.health.enums.HealthEventStatus;
import com.kfarms.health.enums.HealthSeverity;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "health_events")
@Data
public class HealthEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private HealthRule rule;

    @Enumerated(EnumType.STRING)
    private HealthEventStatus status = HealthEventStatus.NEW;

    @Enumerated(EnumType.STRING)
    private HealthSeverity severity;

    @Column(nullable = false)
    private LocalDateTime triggeredAt;

    @Column(length = 1000)
    private String contextNote; // e.g. "Temp: 36°C, Humidity: 82%"

    private LocalDateTime acknowledgedAt;
    private LocalDateTime handledAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "health_event_advice",
            joinColumns = @JoinColumn(name = "event_id")
    )
    @Column(name = "advice_step", length = 300)
    private List<String> adviceSteps;

}
