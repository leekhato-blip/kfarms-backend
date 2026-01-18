package com.kfarms.dto;

import com.kfarms.entity.SupplyCategory;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SuppliesRequestDto {

    @NotBlank(message = "Item name is required")
    private String itemName;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be greater than 0")
    private Integer quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than 0")
    private BigDecimal unitPrice;

    private String supplierName;

    @Size(max = 255, message = "Note cannot exceed 255 characters")
    private String note;

    @PastOrPresent(message = "Date cannot be in the future")
    private LocalDate supplyDate;
}
