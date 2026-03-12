package com.kfarms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RecentActivityDto {
    private String id;
    private String category;
    private String item;
    private Object quantity; // can be Integer, Double, or String "-"
    private String status;
    private LocalDateTime date;
}
