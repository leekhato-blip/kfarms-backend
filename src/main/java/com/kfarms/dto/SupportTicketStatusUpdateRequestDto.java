package com.kfarms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupportTicketStatusUpdateRequestDto {

    @NotBlank(message = "Status is required")
    private String status;
}
