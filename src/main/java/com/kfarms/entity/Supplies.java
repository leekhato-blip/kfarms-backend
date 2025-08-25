package com.kfarms.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;

// PURCHASES

@Data
@Entity
@Table(name = "Supplies")
@AllArgsConstructor
@NoArgsConstructor
public class Supplies extends Auditable{
    @Id
    @GeneratedValue private Long id;
    private String itemName;
    private int quantity;
    private double price;
    private String supplier;
    private LocalDate supplyDate;
}
