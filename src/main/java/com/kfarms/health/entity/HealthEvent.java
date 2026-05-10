package com.kfarms.health.entity;

import com.kfarms.tenant.entity.Tenant;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    private HealthEventStatus status = HealthEventStatus.NEW;

    @Enumerated(EnumType.STRING)
    private HealthSeverity severity;

    @Column(nullable = false)
    private LocalDateTime triggeredAt;

    @Column(nullable = false)
    private String sourceKey;  

    @Column(length = 1000)
    private String contextNote;

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
