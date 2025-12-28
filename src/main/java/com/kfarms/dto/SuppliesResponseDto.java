package com.kfarms.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kfarms.entity.SupplyCategory;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
// PURCHASES
public class SuppliesResponseDto {
    private Long id;
    private String itemName;
    private String category;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String supplierName;
    private String note;
    private LocalDate date;

    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
