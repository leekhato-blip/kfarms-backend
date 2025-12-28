package com.kfarms.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "recurring_task_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecurringTaskRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;                   // "Feed Layers", "Collect Eggs"
    private String entityType;              // "LIVESTOCK", "POND", "FARM"
    private Long entityId;                  // optional
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    private TaskType taskType;              // FEED, COLLECT, etc.

    @ElementCollection
    @CollectionTable(name="recurring_times", joinColumns=@JoinColumn(name="rule_id"))
    @Column(name="time")
    private List<LocalTime> times;          // e.g. 08:00, 13:00, 18:00
}
