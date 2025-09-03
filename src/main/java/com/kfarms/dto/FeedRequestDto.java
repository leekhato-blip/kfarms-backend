package com.kfarms.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class FeedRequestDto {
    private String batchType; // LAYER, FISH
    private Long batchId;
    private  String feedName;
    private int quantityUsed;
    private String notes;
    private LocalDate date;
}
