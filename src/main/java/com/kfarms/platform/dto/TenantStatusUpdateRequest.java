package com.kfarms.platform.dto;

import com.kfarms.tenant.entity.TenantStatus;
import lombok.Data;

@Data
public class TenantStatusUpdateRequest {
    private TenantStatus status; // ACTIVE / SUSPENDED
}