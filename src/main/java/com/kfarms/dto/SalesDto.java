package com.kfarms.dto;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
// SALES/DISTRIBUTIONS
public class SalesDto {
    private Long id;
    private String productType; // e.g. EGGS, FISH
    private int quantity;
    private double price;
    private String buyer;
    private LocalDate saleDate;

    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
