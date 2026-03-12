package com.kfarms.platform.dto;


import com.kfarms.tenant.entity.TenantPlan;
import lombok.Data;

@Data
public class TenantPlanUpdateRequest {
    private TenantPlan plan; // FREE / PRO / ENTERPRISE
}