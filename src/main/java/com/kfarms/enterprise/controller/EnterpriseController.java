package com.kfarms.enterprise.controller;

import com.kfarms.enterprise.entity.EnterpriseSite;
import com.kfarms.enterprise.repository.EnterpriseSiteRepository;
import com.kfarms.enterprise.service.EnterpriseOverviewService;
import com.kfarms.entity.ApiResponse;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.entity.TenantPlan;
import com.kfarms.tenant.service.TenantPermissionCatalog;
import com.kfarms.tenant.service.TenantPermissionService;
import com.kfarms.tenant.service.TenantPlanGuardService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enterprise")
@RequiredArgsConstructor
public class EnterpriseController {

    public record EnterpriseSiteRequest(
            @NotBlank String name,
            @NotBlank String code,
            String location,
            String managerName,
            Boolean active,
            Boolean poultryEnabled,
            Boolean fishEnabled,
            Integer poultryHouseCount,
            Integer pondCount,
            Integer activeBirdCount,
            Integer fishStockCount,
            BigDecimal currentMonthRevenue,
            BigDecimal currentMonthExpenses,
            BigDecimal currentFeedUsageKg,
            Integer projectedEggOutput30d,
            BigDecimal projectedFishHarvestKg,
            BigDecimal currentMortalityRate,
            String notes
    ) {
    }

    private final EnterpriseSiteRepository enterpriseSiteRepository;
    private final EnterpriseOverviewService enterpriseOverviewService;
    private final TenantPlanGuardService tenantPlanGuardService;
    private final TenantPermissionService tenantPermissionService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> overview(Authentication authentication) {
        Tenant tenant = requireEnterpriseTenant(authentication, TenantPermissionCatalog.ENTERPRISE_VIEW, "view the enterprise overview");
        return ResponseEntity.ok(new ApiResponse<>(true, "Enterprise overview loaded.", enterpriseOverviewService.buildOverview(tenant)));
    }

    @GetMapping("/sites")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listSites(Authentication authentication) {
        Tenant tenant = requireEnterpriseTenant(authentication, TenantPermissionCatalog.ENTERPRISE_VIEW, "view enterprise sites");
        List<Map<String, Object>> rows = enterpriseOverviewService.listSites(tenant).stream()
                .map(this::toSiteRow)
                .toList();
        return ResponseEntity.ok(new ApiResponse<>(true, "Enterprise sites loaded.", rows));
    }

    @PostMapping("/sites")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createSite(
            Authentication authentication,
            @RequestBody EnterpriseSiteRequest request
    ) {
        Tenant tenant = requireEnterpriseTenant(authentication, TenantPermissionCatalog.ENTERPRISE_MANAGE, "manage enterprise sites");
        EnterpriseSite site = new EnterpriseSite();
        site.setTenant(tenant);
        applyRequest(site, request);
        site.setCreatedBy(authentication.getName());
        site.setUpdatedBy(authentication.getName());
        EnterpriseSite saved = enterpriseSiteRepository.save(site);
        return ResponseEntity.ok(new ApiResponse<>(true, "Site created.", toSiteRow(saved)));
    }

    @PutMapping("/sites/{siteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateSite(
            @PathVariable Long siteId,
            Authentication authentication,
            @RequestBody EnterpriseSiteRequest request
    ) {
        Tenant tenant = requireEnterpriseTenant(authentication, TenantPermissionCatalog.ENTERPRISE_MANAGE, "manage enterprise sites");
        EnterpriseSite site = enterpriseSiteRepository.findByIdAndTenant_Id(siteId, tenant.getId())
                .orElseThrow(() -> new IllegalArgumentException("Site not found."));
        applyRequest(site, request);
        site.setUpdatedBy(authentication.getName());
        EnterpriseSite saved = enterpriseSiteRepository.save(site);
        return ResponseEntity.ok(new ApiResponse<>(true, "Site updated.", toSiteRow(saved)));
    }

    @DeleteMapping("/sites/{siteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Object>> deleteSite(
            @PathVariable Long siteId,
            Authentication authentication
    ) {
        Tenant tenant = requireEnterpriseTenant(authentication, TenantPermissionCatalog.ENTERPRISE_MANAGE, "manage enterprise sites");
        EnterpriseSite site = enterpriseSiteRepository.findByIdAndTenant_Id(siteId, tenant.getId())
                .orElseThrow(() -> new IllegalArgumentException("Site not found."));
        enterpriseSiteRepository.delete(site);
        return ResponseEntity.ok(new ApiResponse<>(true, "Site removed.", null));
    }

    private Tenant requireEnterpriseTenant(Authentication authentication, String permission, String action) {
        Tenant tenant = tenantPlanGuardService.requireCurrentTenant();
        tenantPlanGuardService.requirePlanAccess(
                tenant,
                TenantPlan.ENTERPRISE,
                "Enterprise controls are available on the Enterprise plan."
        );
        tenantPermissionService.requirePermission(authentication, permission, "You do not have permission to " + action + ".");
        return tenant;
    }

    private void applyRequest(EnterpriseSite site, EnterpriseSiteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Site details are required.");
        }
        boolean poultryEnabled = request.poultryEnabled() == null || Boolean.TRUE.equals(request.poultryEnabled());
        boolean fishEnabled = Boolean.TRUE.equals(request.fishEnabled());
        if (!poultryEnabled && !fishEnabled) {
            throw new IllegalArgumentException("Select at least one module for the site.");
        }

        site.setName(requireText(request.name(), "Site name is required."));
        site.setCode(normalizeCode(request.code()));
        site.setLocation(blankToNull(request.location()));
        site.setManagerName(blankToNull(request.managerName()));
        site.setActive(request.active() == null || Boolean.TRUE.equals(request.active()));
        site.setPoultryEnabled(poultryEnabled);
        site.setFishEnabled(fishEnabled);
        site.setPoultryHouseCount(nonNegative(request.poultryHouseCount()));
        site.setPondCount(nonNegative(request.pondCount()));
        site.setActiveBirdCount(nonNegative(request.activeBirdCount()));
        site.setFishStockCount(nonNegative(request.fishStockCount()));
        site.setCurrentMonthRevenue(nonNegativeMoney(request.currentMonthRevenue()));
        site.setCurrentMonthExpenses(nonNegativeMoney(request.currentMonthExpenses()));
        site.setCurrentFeedUsageKg(nonNegativeMoney(request.currentFeedUsageKg()));
        site.setProjectedEggOutput30d(nonNegative(request.projectedEggOutput30d()));
        site.setProjectedFishHarvestKg(nonNegativeMoney(request.projectedFishHarvestKg()));
        site.setCurrentMortalityRate(nonNegativeMoney(request.currentMortalityRate()));
        site.setNotes(blankToNull(request.notes()));
    }

    private Map<String, Object> toSiteRow(EnterpriseSite site) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", site.getId());
        row.put("name", site.getName());
        row.put("code", site.getCode());
        row.put("location", site.getLocation());
        row.put("managerName", site.getManagerName());
        row.put("active", site.getActive());
        row.put("poultryEnabled", site.getPoultryEnabled());
        row.put("fishEnabled", site.getFishEnabled());
        row.put("poultryHouseCount", site.getPoultryHouseCount());
        row.put("pondCount", site.getPondCount());
        row.put("activeBirdCount", site.getActiveBirdCount());
        row.put("fishStockCount", site.getFishStockCount());
        row.put("currentMonthRevenue", site.getCurrentMonthRevenue());
        row.put("currentMonthExpenses", site.getCurrentMonthExpenses());
        row.put("currentFeedUsageKg", site.getCurrentFeedUsageKg());
        row.put("projectedEggOutput30d", site.getProjectedEggOutput30d());
        row.put("projectedFishHarvestKg", site.getProjectedFishHarvestKg());
        row.put("currentMortalityRate", site.getCurrentMortalityRate());
        row.put("notes", site.getNotes());
        return row;
    }

    private String requireText(String value, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalizeCode(String value) {
        String normalized = requireText(value, "Site code is required.")
                .toUpperCase()
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Site code is invalid.");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private Integer nonNegative(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private BigDecimal nonNegativeMoney(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : value;
    }
}
