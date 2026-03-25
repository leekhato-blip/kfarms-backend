package com.kfarms.platform.dto;

import com.kfarms.tenant.entity.TenantPlan;
import com.kfarms.tenant.entity.TenantStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TenantAdminDetailsDto {

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
    private LocalDateTime lastActivityAt;

    // optional usage metrics
    private long livestockCount;
    private long fishPondCount;
    private long feedItemCount;
    private long salesCount;

    private List<String> enabledModules;
    private List<TenantMemberSummaryDto> members;
}
