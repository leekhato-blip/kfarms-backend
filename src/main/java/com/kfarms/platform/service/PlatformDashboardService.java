package com.kfarms.platform.service;

import com.kfarms.platform.dto.PlatformAppPortfolioDto;
import com.kfarms.platform.dto.PlatformDashboardOverviewDto;

public interface PlatformDashboardService {

    PlatformDashboardOverviewDto getOverview();

    PlatformAppPortfolioDto getAppPortfolio();
}
