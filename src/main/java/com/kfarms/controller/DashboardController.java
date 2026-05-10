package com.kfarms.controller;

import com.kfarms.dto.DashboardSummaryDto;
import com.kfarms.dto.RecentActivityDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.service.DashboardService;
import com.kfarms.service.FinanceService;
import com.kfarms.service.impl.RecentActivitiesService;
import com.kfarms.tenant.entity.TenantPlan;
import com.kfarms.tenant.service.TenantPlanGuardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final FinanceService financeService;
    private final RecentActivitiesService recentActivitiesService;
    private final TenantPlanGuardService tenantPlanGuardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getSummary() {
        DashboardSummaryDto summary = dashboardService.getSummary();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Dashboard summary fetched successfully", summary)
        );
    }

    @GetMapping("/finance-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFinanceSummary() {
        tenantPlanGuardService.requireCurrentTenantPlanAccess(
                TenantPlan.PRO,
                "Revenue analytics are available on the Pro plan."
        );
        Map<String, Object> summary = financeService.getMonthlyFinance();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Finance summary fetched successfully", summary)
        );
    }

    @GetMapping("/recent-activities")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public List<RecentActivityDto> getRecentActivities(@RequestParam(defaultValue = "3") int limit) {
        return recentActivitiesService.getRecentActivities(limit);
    }
}
