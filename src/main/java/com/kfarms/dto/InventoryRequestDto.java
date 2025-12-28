package com.kfarms.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class InventoryRequestDto {

    @NotBlank(message = "Item name is required")
    @Size(max = 100, message = "Item name cannot exceed 100 characters")
    private String itemName;

    @NotBlank(message = "Category is required")
    private String category; // e.g. FEED, MEDICINE, TOOL

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    @NotBlank(message = "Unit is required")
    private String unit;  // e.g. kg, bags, litres

    @Min(value = 0, message = "Minimum threshold cannot be negative")
    private Integer minThreshold;

    @Size(max = 255, message = "Note cannot exceed 255 characters")
    private String note;

    @PastOrPresent(message = "Last updated date cannot be in the future")
    private LocalDate lastUpdated;
}
