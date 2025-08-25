package com.kfarms.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeedDto {
    private Long id;
    private String batchType; // LAYER, FISH
    private Long batchId;
    private String feedName;
    private int quantityUsed;
    private LocalDate date;

    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
