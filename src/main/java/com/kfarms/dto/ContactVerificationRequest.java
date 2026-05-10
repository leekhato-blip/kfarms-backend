package com.kfarms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ContactVerificationRequest(
        @NotBlank @Email String email,
        String emailCode,
        String phoneCode
) {
}
