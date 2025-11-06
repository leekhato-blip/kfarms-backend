package com.kfarms.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

// SALES/DISTRIBUTIONS
@Data
@Entity
@Table(name = "sales")
@RequiredArgsConstructor
public class Sales extends Auditable {

    @Id
    @GeneratedValue private Long id;

    @Column(nullable = false)
    private String itemName; // e.g., "Layer Eggs", "Fish 1kg"

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SalesCategory category; // e.g. EGGS, FISH

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false,precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false,precision = 19, scale = 2)
    private BigDecimal totalPrice;

    private String note; // nullable - returns null if not provided
    private String buyer; // nullable - returns "Walk-in customer" if not provided

    private LocalDate salesDate = LocalDate.now(); // default today
}
