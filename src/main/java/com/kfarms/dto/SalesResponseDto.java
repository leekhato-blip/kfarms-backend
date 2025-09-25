package com.kfarms.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
// SALES/DISTRIBUTIONS
public class SalesResponseDto {
    private Long id;
    private String itemName;
    private String category; // e.g. EGGS, FISH
    private int quantity;
    private double unitPrice;
    private double totalPrice;
    private String buyer;
    private String note;
    private LocalDate date;

    // audit
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
