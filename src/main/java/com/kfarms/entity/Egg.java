package com.kfarms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "Eggs")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Egg extends Auditable{
    @Id
    @GeneratedValue private Long id;

    @ManyToOne
    private Layer layer;

    private LocalDate collectionDate;
    private int quantity;
    private String notes;
}
