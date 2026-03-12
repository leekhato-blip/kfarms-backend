package com.kfarms.platform.controller;

import com.kfarms.entity.ApiResponse;
import com.kfarms.platform.dto.PlatformDashboardOverviewDto;
import com.kfarms.platform.service.PlatformDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/dashboard")
@RequiredArgsConstructor
public class PlatformDashboardController {

    private final PlatformDashboardService platformDashboardService;

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<PlatformDashboardOverviewDto>> getOverview() {
        PlatformDashboardOverviewDto overview = platformDashboardService.getOverview();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Platform overview fetched successfully",
                        overview
                )
        );
    }
}
