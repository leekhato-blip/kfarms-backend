package com.kfarms.platform.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantMemberSummaryDto {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private boolean active;
}
