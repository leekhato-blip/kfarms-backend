package com.kfarms.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InventoryRequestDto {

    @NotBlank(message = "Item name is required")
    @Size(max = 100, message = "Item name cannot exceed 100 characters")
    private String itemName;

    @NotBlank(message = "Category is required")
    private String category; // e.g. FEED, MEDICINE, TOOL

    @Size(max = 80, message = "SKU cannot exceed 80 characters")
    private String sku;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    @NotBlank(message = "Unit is required")
    private String unit;  // e.g. kg, bags, litres

    @DecimalMin(value = "0.0", inclusive = true, message = "Unit cost cannot be negative")
    private BigDecimal unitCost;

    @Min(value = 0, message = "Minimum threshold cannot be negative")
    private Integer minThreshold;

    @Size(max = 120, message = "Supplier name cannot exceed 120 characters")
    private String supplierName;

    @Size(max = 120, message = "Storage location cannot exceed 120 characters")
    private String storageLocation;

    @Size(max = 255, message = "Note cannot exceed 255 characters")
    private String note;

    @PastOrPresent(message = "Last updated date cannot be in the future")
    private LocalDate lastUpdated;
}
