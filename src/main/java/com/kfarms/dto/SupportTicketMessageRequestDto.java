package com.kfarms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupportTicketMessageRequestDto {

    @NotBlank(message = "Reply body is required")
    @Size(max = 2500, message = "Reply body must not exceed 2500 characters")
    private String body;
}
