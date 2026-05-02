package com.kfarms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MortalityRecordRequestDto {
    @Min(value = 1, message = "Mortality count must be at least 1")
    private Integer count;

    @PastOrPresent(message = "Mortality date cannot be in the future")
    private LocalDate mortalityDate;

    @Size(max = 255, message = "Note cannot exceed 255 characters")
    private String note;
}
