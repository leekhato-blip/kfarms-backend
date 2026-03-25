package com.kfarms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerificationResendRequest(
        @NotBlank @Email String email,
        @NotBlank String channel
) {
}
