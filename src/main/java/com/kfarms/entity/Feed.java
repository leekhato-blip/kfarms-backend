package com.kfarms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;


@Data
@Entity
@Table(name = "Feed")
@RequiredArgsConstructor
public class Feed extends Auditable{
    @Id
    @GeneratedValue private Long id;
    private String batchType; // LAYER, FISH
    private Long batchId;
    private String feedName;
    @Column(nullable = true) // explicitly nullable
    private String notes;
    private int quantityUsed;
    private LocalDate date;
}
