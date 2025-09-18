package com.kfarms.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SalesRequestDto {
    private String itemName;
    private String category;
    private Integer quantity;
    private Double unitPrice;
    private String buyer;
    private String notes;
    private LocalDate date; // optional - defaults to today
}
