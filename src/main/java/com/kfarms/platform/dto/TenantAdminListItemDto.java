package com.kfarms.platform.dto;


import com.kfarms.tenant.entity.TenantPlan;
import com.kfarms.tenant.entity.TenantStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TenantAdminListItemDto {

    private Long id;
    private String name;
    private String slug;
    private TenantPlan plan;
    private TenantStatus status;
    private String appId;
    private String appName;
    private String appLifecycle;

    private String ownerEmail;
    private int memberCount;

    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt; // optional, can be derived later
}
