package com.kfarms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;


@Data
@Entity
@Table(name = "feed")
@RequiredArgsConstructor
public class Feed extends Auditable{
    @Id
    @GeneratedValue private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedBatchType batchType; // LAYER, FISH

    @Column(nullable = false)
    private Long batchId;

    @Column(nullable = false)
    private String feedName;

    @Column(nullable = true) // explicitly nullable
    private String note;

    @Column(nullable = false)
    private Integer quantityUsed;

    private LocalDate date = LocalDate.now(); // default date
}
