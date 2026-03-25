package com.kfarms.platform.controller;

import com.kfarms.entity.ApiResponse;
import com.kfarms.platform.dto.PlatformAppPortfolioDto;
import com.kfarms.platform.service.PlatformDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/apps")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformAppController {

    private final PlatformDashboardService platformDashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<PlatformAppPortfolioDto>> getAppPortfolio() {
        PlatformAppPortfolioDto portfolio = platformDashboardService.getAppPortfolio();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Platform app portfolio fetched successfully",
                        portfolio
                )
        );
    }
}
