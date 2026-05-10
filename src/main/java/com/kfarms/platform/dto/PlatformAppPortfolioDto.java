package com.kfarms.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformAppPortfolioDto {

    private long totalApps;
    private long liveApps;
    private long plannedApps;
    private long totalWorkspaces;
    private long totalOperators;
    private List<PlatformAppSummaryDto> apps;
}
