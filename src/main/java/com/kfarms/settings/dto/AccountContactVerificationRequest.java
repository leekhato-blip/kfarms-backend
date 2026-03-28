package com.kfarms.settings.dto;

public record AccountContactVerificationRequest(
        String emailCode,
        String phoneCode
) {
}
