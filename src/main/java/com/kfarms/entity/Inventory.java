package com.kfarms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(
        name = "Inventory",
        uniqueConstraints = @UniqueConstraint(columnNames = {"itemName", "category"})
)
@Data
@RequiredArgsConstructor
public class Inventory extends Auditable{
    @Id
    @GeneratedValue private Long id;

    @Column(nullable = false)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryCategory category; // e.g. FEED, MEDICINE, TOOL

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private int minThreshold = 0;

    @Column(nullable = false)
    private String unit;  // e.g.  kg, bags, litres

    private String note; // e.g. reserved for pond 3

    @Column(nullable = false)
    private LocalDate lastUpdated = LocalDate.now(); // default today
}
