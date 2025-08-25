package com.kfarms.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryDto {
    private Long id;
    private String item;
    private String category; // e.g. FEED, VACCINE, TOOL
    private int quantity;
    private String unit;  // e.g.  kg, bags, litres
    private LocalDate lastUpdated;

    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
