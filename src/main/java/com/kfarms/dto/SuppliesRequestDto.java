package com.kfarms.dto;

import com.kfarms.entity.SupplyCategory;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SuppliesRequestDto {
    private String itemName;
    private String category;
    private int quantity;
    private double unitPrice;
    private String supplierName;
    private String note;
    private LocalDate date; // optional, defaults to today
}
