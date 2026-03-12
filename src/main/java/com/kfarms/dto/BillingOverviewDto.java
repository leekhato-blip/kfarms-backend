package com.kfarms.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BillingOverviewDto {
    private String planId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String interval;
    private String provider;
    private LocalDate nextBillingDate;
    private Boolean cancelAtPeriodEnd;
    private String subscriptionReference;
    private String paymentMethodBrand;
    private String paymentMethodLast4;
    private LocalDateTime updatedAt;
}
