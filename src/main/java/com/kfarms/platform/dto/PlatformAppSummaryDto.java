package com.kfarms.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformAppSummaryDto {

    private String id;
    private String name;
    private String category;
    private String lifecycle;
    private String headline;
    private String description;
    private String consolePath;
    private String workspacePath;
    private long tenantCount;
    private long activeTenantCount;
    private long suspendedTenantCount;
    private long operatorCount;
    private BigDecimal revenueGenerated;
    private String revenueCurrency;
    private int sortOrder;
    private List<String> capabilities;
    private String source;
}
