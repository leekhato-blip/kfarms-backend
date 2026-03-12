package com.kfarms.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BillingInvoiceDto {
    private String id;
    private LocalDateTime createdAt;
    private String description;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String reference;
    private String downloadUrl;
}
