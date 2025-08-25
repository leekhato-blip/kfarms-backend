package com.kfarms.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "Inventory")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Inventory extends Auditable{
    @Id
    @GeneratedValue private Long id;
    private String item;
    private String category; // e.g. FEED, VACCINE, TOOL
    private int quantity;
    private String unit;  // e.g.  kg, bags, litres
    private LocalDate lastUpdated;
}
