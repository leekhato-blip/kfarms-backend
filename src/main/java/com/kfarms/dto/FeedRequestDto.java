package com.kfarms.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeedRequestDto {

    @NotBlank(message = "Batch type is required")
    @JsonAlias("type")
    private String batchType; // LAYER, FISH, etc.

    private Long batchId;

    @JsonAlias({"name", "itemName"})
    @Size(max = 100, message = "Feed name cannot exceed 100 characters")
    private String feedName;

    @JsonAlias("quantity")
    @Min(value = 1, message = "Quantity must be greater than 0")
    private Integer quantityUsed;

    @DecimalMin(value = "0.0", inclusive = true, message = "Unit cost cannot be negative")
    private BigDecimal unitCost;

    @Size(max = 255, message = "Note cannot exceed 255 characters")
    private String note;

    @PastOrPresent(message = "Date cannot be in the future")
    private LocalDate date;
}
