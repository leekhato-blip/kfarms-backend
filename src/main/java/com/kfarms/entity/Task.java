package com.kfarms.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Setter
@Getter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Task extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskType type = TaskType.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskSource source = TaskSource.MANUAL;

    private Integer priority = 3;

    private LocalDateTime dueDate;

    // Loose coupling to domain entities (optional)
    private  String relatedEntityType; // e.g., "POND", "LIVESTOCK", "FEED"
    private Long relatedEntityId;





}
