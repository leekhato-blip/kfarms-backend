package com.kfarms.dto;

import com.kfarms.entity.LivestockType;
import com.kfarms.entity.SourceType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data

public class LivestockRequest {
    @NotNull(message = "Batch name cannot be null")
    @Size(min = 1, max = 50, message = "Batch name should have a length between 1 and 50 characters")
    private String batchName;

    @Min(value = 1, message = "Quantity must be greater than 0")
    private int quantity;

    @NotNull(message = "Livestock type cannot be null")
    private LivestockType type;

    @NotNull(message = "Arrival date cannot be null")
    @FutureOrPresent(message = "Arrival date cannot be in the past")
    private LocalDate arrivalDate;

    @NotNull(message = "Source type cannot be null")
    private SourceType sourceType;       // FARM_BIRTH or SUPPLIER

    @Min(value = 0, message = "Starting age in weeks must be greater than or equal to 0")
    private Integer startingAgeInWeeks;  // optional; default 0 for FARM_BIRTH

    @Min(value = 0, message = "Mortality cannot be negative")
    private Integer mortality;           // optional; default 0

    private String notes;
}
