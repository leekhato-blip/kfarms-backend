package com.kfarms.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;

// SALES/DISTRIBUTIONS
@Data
@Entity
@Table(name = "Sales")
@AllArgsConstructor
@NoArgsConstructor
public class Sales extends Auditable{
    @Id
    @GeneratedValue private Long id;
    private String productType; // e.g. EGGS, FISH
    private int quantity;
    private double price;
    private String buyer;
    private LocalDate saleDate;
}
