package com.kfarms.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class FishHatchRequestDto {

    @NotNull(message = "Pond ID is required")
    private Long pondId;

    @NotNull(message = "Hatch date is required")
    @PastOrPresent(message = "Hatch date cannot be in the future")
    private LocalDate hatchDate;

    @Min(value = 0, message = "Male count cannot be negative")
    private int maleCount;

    @Min(value = 0, message = "Female count cannot be negative")
    private int femaleCount;

    @Min(value = 0, message = "Quantity hatched cannot be negative")
    private int quantityHatched;

    @Size(max = 255, message = "Note cannot exceed 255 characters")
    private String note;
}
