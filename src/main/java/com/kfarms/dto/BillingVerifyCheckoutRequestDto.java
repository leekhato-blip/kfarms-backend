package com.kfarms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BillingVerifyCheckoutRequestDto {

    @NotBlank(message = "Reference is required")
    private String reference;

    @NotBlank(message = "Plan ID is required")
    private String planId;

    private String billingInterval;
}
