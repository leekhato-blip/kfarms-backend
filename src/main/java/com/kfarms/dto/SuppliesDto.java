package com.kfarms.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
// PURCHASES
public class SuppliesDto {
    private Long id;
    private String itemName;
    private int quantity;
    private double price;
    private String supplier;
    private LocalDate supplyDate;

    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
