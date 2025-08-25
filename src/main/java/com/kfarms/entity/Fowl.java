package com.kfarms.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;


@Data
@Entity
@Table(name = "Fowl")
@AllArgsConstructor
@NoArgsConstructor
public class Fowl extends Auditable{
    @Id
    @GeneratedValue private Long id;
    private String batchName;
    private int quantity;
    private LocalDate arrivalDate;
    private int ageInWeeks;
    private String notes;
}
