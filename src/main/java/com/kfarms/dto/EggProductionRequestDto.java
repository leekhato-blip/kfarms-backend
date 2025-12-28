package com.kfarms.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class EggProductionRequestDto {

    @NotNull(message = "Batch ID is required")
    private Long batchId;

    @PastOrPresent(message = "Collection date cannot be in the future")
    private LocalDate collectionDate;

    @NotNull(message = "Good eggs count is required")
    @Min(value = 0, message = "Good eggs cannot be negative")
    private int goodEggs;

    @NotNull(message = "Damaged eggs count is required")
    @Min(value = 0, message = "Damaged eggs cannot be negative")
    private int damagedEggs;

    @Size(max = 255, message = "Note cannot exceed 255 characters")
    private String note;
}
