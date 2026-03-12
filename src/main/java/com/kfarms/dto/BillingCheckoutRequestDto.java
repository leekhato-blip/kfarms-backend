package com.kfarms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BillingCheckoutRequestDto {

    @NotBlank(message = "Plan ID is required")
    private String planId;

    private String successUrl;

    private String cancelUrl;

    @Email(message = "Customer email must be valid")
    private String customerEmail;
}
