package com.kfarms.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class FeedRequestDto {

    @NotBlank(message = "Batch type is required")
    private String batchType; // LAYER, FISH, etc.

    @NotNull(message = "Batch ID is required")
    private Long batchId;

    @NotBlank(message = "Feed name is required")
    @Size(max = 100, message = "Feed name cannot exceed 100 characters")
    private String feedName;

    @NotNull(message = "Quantity used is required")
    @Min(value = 1, message = "Quantity must be greater than 0")
    private Integer quantityUsed;

    @Size(max = 255, message = "Note cannot exceed 255 characters")
    private String note;

    @PastOrPresent(message = "Date cannot be in the future")
    private LocalDate date;
}
