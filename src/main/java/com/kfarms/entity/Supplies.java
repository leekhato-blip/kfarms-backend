package com.kfarms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

// PURCHASES

@Data
@Entity
@Table(name = "Supplies")
@RequiredArgsConstructor
public class Supplies extends Auditable{
    @Id
    @GeneratedValue private Long id;

    @Column(nullable = false)
    private String itemName; // e.g., "Layer Feed", Vaccine

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupplyCategory category; // FEED, LIVESTOCK, FISH, MEDICINE, EQUIPMENT, OTHER

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private double unitPrice;

    @Column(nullable = false)
    private double totalPrice; // auto = quantity + unitPrice

    private String supplierName;
    private String note;

    @Column(nullable = false)
    private LocalDate date = LocalDate.now(); // default today
}
