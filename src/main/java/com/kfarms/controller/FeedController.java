package com.kfarms.controller;


import com.kfarms.dto.FeedRequestDto;
import com.kfarms.dto.FeedResponseDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.service.FeedService;
import com.kfarms.tenant.entity.TenantPlan;
import com.kfarms.tenant.service.TenantPlanGuardService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/feeds")
public class FeedController {
    private final FeedService service;
    private final TenantPlanGuardService tenantPlanGuardService;

    // CREATE - add new feed
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FeedResponseDto>> create(
            @Valid @RequestBody FeedRequestDto dto) {
        FeedResponseDto saved = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Feed saved successfully", saved));
    }

    // READ - get all feeds
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String batchType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, defaultValue = "false") Boolean deleted
            ) {
        log.debug("Fetching feeds with batchType='{}', date='{}', deleted={}", batchType, date, deleted);
        if (Boolean.TRUE.equals(deleted)) {
            tenantPlanGuardService.requireCurrentTenantPlanAccess(
                    TenantPlan.PRO,
                    "Trash restore is available on the Pro plan."
            );
        }

        Map<String, Object> response = service.getAll(page, size, batchType, date, deleted);
        return ResponseEntity.ok(new ApiResponse<>(true, "Feeds fetched successfully", response));
    }

    // READ - get feed by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<FeedResponseDto>> getById(@PathVariable Long id) {
        FeedResponseDto dto = service.getById(id);
        if (dto != null) {
            return ResponseEntity.ok(new ApiResponse<>(true, "Feed detail fetched", dto));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Feed with ID: " + id + " not found", null));
        }
    }

    // UPDATE - update existing Feed
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FeedResponseDto>> update(
            @PathVariable Long id,
            @RequestBody FeedRequestDto request
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String updatedBy = auth.getName();
        FeedResponseDto response = service.update(id, request, updatedBy);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Feed updated successfully", response)
        );
    }

    // DELETE - delete existing feed
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String deletedBy = auth != null ? auth.getName() : "SYSTEM";
        service.delete(id, deletedBy);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Feed record deleted successfully", null)
        );
    }

    // PERMANENT DELETE
    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> permanentDelete(@PathVariable Long id) {
        tenantPlanGuardService.requireCurrentTenantPlanAccess(
                TenantPlan.PRO,
                "Trash restore is available on the Pro plan."
        );
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String deletedBy = auth != null ? auth.getName() : "SYSTEM";
        service.permanentDelete(id, deletedBy);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Feed record deleted permanently", null)
        );
    }

    // RESTORE
    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> restore(@PathVariable Long id) {
        tenantPlanGuardService.requireCurrentTenantPlanAccess(
                TenantPlan.PRO,
                "Trash restore is available on the Pro plan."
        );
        service.restore(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Feed record restored", null)
        );
    }


    // SUMMARY - dashboard, reports and analysis
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary() {
        Map<String, Object> summary = service.getSummary();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Feed summary fetched", summary)
        );
    }
}
