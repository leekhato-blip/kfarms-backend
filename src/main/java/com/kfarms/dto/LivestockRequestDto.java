package com.kfarms.dto;

import com.kfarms.entity.LivestockType;
import com.kfarms.entity.SourceType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data

public class LivestockRequestDto {
    @NotBlank(message = "Batch name is required")
    @Size(min = 1, max = 50, message = "Batch name cannot exceed 50 characters")
    private String batchName;

    @NotNull(message = "Current stock is required")
    @Min(value = 1, message = "Quantity must be greater than 0")
    private Integer currentStock;

    @NotNull(message = "Livestock type is required")
    private LivestockType type;

    @PastOrPresent(message = "Arrival date cannot be in the future")
    private LocalDate arrivalDate;

    @NotNull(message = "Source type is required")
    private SourceType sourceType;       // FARM_BIRTH or SUPPLIER

    @Min(value = 0, message = "Starting age in weeks cannot be negative")
    private Integer startingAgeInWeeks = 0;  // optional; default 0 for FARM_BIRTH

    @Min(value = 0, message = "Mortality cannot be negative")
    private Integer mortality;           // optional; default 0

    @Size(max = 255, message = "Note cannot exceed 255 characters")
    private String note;
}
