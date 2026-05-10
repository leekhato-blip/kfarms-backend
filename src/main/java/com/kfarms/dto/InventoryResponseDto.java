package com.kfarms.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryResponseDto {
    private Long id;
    private String itemName;
    private String sku;
    private String category; // e.g. FEED, MEDICINE, TOOL
    private Integer quantity;
    private String unit;  // e.g.  kg, bags, litres
    private BigDecimal unitCost;
    private BigDecimal totalValue;
    private int minThreshold;
    private String supplierName;
    private String storageLocation;
    private String note;
    private LocalDate lastUpdated;

    // audits
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
