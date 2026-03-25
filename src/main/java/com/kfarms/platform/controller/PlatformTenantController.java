package com.kfarms.platform.controller;


import com.kfarms.entity.ApiResponse;
import com.kfarms.platform.dto.TenantAdminDetailsDto;
import com.kfarms.platform.dto.TenantAdminListItemDto;
import com.kfarms.platform.dto.TenantPlanUpdateRequest;
import com.kfarms.platform.dto.TenantStatusUpdateRequest;
import com.kfarms.platform.service.PlatformTenantService;
import com.kfarms.tenant.entity.TenantPlan;
import com.kfarms.tenant.entity.TenantStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform/tenants")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformTenantController {

    private final PlatformTenantService platformTenantService;


    @GetMapping
    public ResponseEntity<ApiResponse<Page<TenantAdminListItemDto>>> listTenants(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TenantStatus status,
            @RequestParam(required = false) TenantPlan plan,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Pageable pageable = PageRequest.of(page, size);

        Page<TenantAdminListItemDto> result =
                platformTenantService.searchTenants(search, status, plan, pageable);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Tenants fetched successfully",
                        result
                )
        );
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantAdminDetailsDto>> getTenantDetails(
            @PathVariable Long tenantId
    ) {
        TenantAdminDetailsDto dto =
                platformTenantService.getTenantDetails(tenantId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Tenant details fetched successfully",
                        dto
                )
        );
    }

    @PatchMapping("/{tenantId}/plan")
    public ResponseEntity<ApiResponse<Void>> updatePlan(
            @PathVariable Long tenantId,
            @RequestBody TenantPlanUpdateRequest request
    ) {

        platformTenantService.updateTenantPlan(tenantId, request.getPlan());

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Tenant plan updated successfully",
                        null
                )
        );
    }

    @PatchMapping("/{tenantId}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable Long tenantId,
            @RequestBody TenantStatusUpdateRequest request
    ) {

        platformTenantService.updateTenantStatus(tenantId, request.getStatus());

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Tenant status updated successfully",
                        null
                )
        );
    }

}
