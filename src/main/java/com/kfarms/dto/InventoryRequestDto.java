package com.kfarms.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class InventoryRequestDto {
    private String itemName;
    private String category; // e.g. FEED, MEDICINE, TOOL
    private int quantity;
    private String unit;  // e.g.  kg, bags, litres
    private int minThreshold;
    private String note;
    private LocalDate lastUpdated;

}
