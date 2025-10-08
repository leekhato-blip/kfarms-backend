package com.kfarms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EggProductionRequestDto {

    @NotNull(message = "Batch ID is required")
    private Long livestockId;

    @NotNull(message = "Production date is required")
    private LocalDate collectionDate;

    @Min(value = 0, message = "Good eggs cannot be negative")
    private Integer goodEggs;

    @Min(value = 0, message = "Damaged eggs cannot be negative")
    private Integer damagedEggs;

    private String note;
}
