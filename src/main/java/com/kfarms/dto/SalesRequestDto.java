package com.kfarms.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SalesRequestDto {
    private String itemName;
    private String category;
    private Integer quantity;
    private double unitPrice;
    private String buyer;
    private String note;
    private LocalDate date; // optional - defaults to today
}
