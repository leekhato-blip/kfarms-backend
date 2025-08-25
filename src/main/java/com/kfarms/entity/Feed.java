package com.kfarms.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;


@Data
@Entity
@Table(name = "Feed")
@AllArgsConstructor
@NoArgsConstructor
public class Feed extends Auditable{
    @Id
    @GeneratedValue private Long id;
    private String batchType; // LAYER, FISH
    private Long batchId;
    private String feedName;
    private int quantityUsed;
    private LocalDate date;
}
