package com.kfarms.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "FishPond")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FishPond extends Auditable{
    @Id
    @GeneratedValue private Long id;
    private String name;  // e.g. "Tank 1"
    private int quantity;
    private int capacity;
    private LocalDate lastWaterChangeDate;
    private String status; // ACTIVE, EMPTY, MAINTENANCE
}
