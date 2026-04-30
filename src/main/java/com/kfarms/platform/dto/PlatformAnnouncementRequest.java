package com.kfarms.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PlatformAnnouncementRequest {

    @NotBlank
    @Size(max = 120)
    private String title;

    @NotBlank
    @Size(max = 280)
    private String message;

    @NotBlank
    private String audience = "ALL_ACTIVE";

    private List<Long> tenantIds = List.of();
}
