package com.kfarms.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class FishHatchRequestDto {
    private Long pondId;
    private LocalDate hatchDate;
    private int maleCount;
    private int femaleCount;
    private int quantityHatched;
    private String note;
}
