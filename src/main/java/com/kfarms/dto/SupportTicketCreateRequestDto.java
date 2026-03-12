package com.kfarms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupportTicketCreateRequestDto {

    @NotBlank(message = "Ticket subject is required")
    @Size(max = 140, message = "Ticket subject must not exceed 140 characters")
    private String subject;

    @NotBlank(message = "Category is required")
    @Size(max = 120, message = "Category must not exceed 120 characters")
    private String category;

    @NotBlank(message = "Priority is required")
    private String priority;

    @NotBlank(message = "Description is required")
    @Size(max = 2500, message = "Description must not exceed 2500 characters")
    private String description;
}
