package com.kfarms.controller;

import com.kfarms.dto.InventoryAdjustmentRequestDto;
import com.kfarms.dto.InventoryRequestDto;
import com.kfarms.dto.InventoryResponseDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.InventoryCategory;
import com.kfarms.service.InventoryService;
import com.kfarms.tenant.entity.TenantPlan;
import com.kfarms.tenant.service.TenantPlanGuardService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService service;
    private final TenantPlanGuardService tenantPlanGuardService;

    public InventoryController(InventoryService service, TenantPlanGuardService tenantPlanGuardService){
        this.service = service;
        this.tenantPlanGuardService = tenantPlanGuardService;
    }

    // CREATE - add new inventory item
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<InventoryResponseDto>> create(
            @Valid @RequestBody InventoryRequestDto dto) {
        InventoryResponseDto saved = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(true, "Inventory record saved successfully", saved)
        );
    }

    // READ - get all with filtering & pagination
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String itemName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "false") Boolean deleted,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lastUpdated
            ){
        if (Boolean.TRUE.equals(deleted)) {
            tenantPlanGuardService.requireCurrentTenantPlanAccess(
                    TenantPlan.PRO,
                    "Trash restore is available on the Pro plan."
            );
        }
        Map<String, Object> response = service.getAll(page, size, itemName, category, status, lastUpdated, deleted);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "All inventory fetched successfully", response)
        );
    }

    // READ - get inventory by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<InventoryResponseDto>> getById(@PathVariable Long id){
        InventoryResponseDto dto = service.getById(id);
        if (dto != null) {
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Inventory record fetched successfully", dto)
            );
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Inventory with ID: " + id + " not found", null));
        }
    }

    // UPDATE - update existing inventory item with ID
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<InventoryResponseDto>> udpate(
            @PathVariable Long id,
            @Valid @RequestBody InventoryRequestDto request
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String updatedBy = auth.getName();
        InventoryResponseDto response = service.update(id, request, updatedBy);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Inventory updated successfully", response)
        );

    }

    @GetMapping("/watchlist")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> feedWatchlist() {
        tenantPlanGuardService.requireCurrentTenantPlanAccess(
                TenantPlan.PRO,
                "Feed watchlists are available on the Pro plan."
        );
        List<Map<String, Object>> watchlist = service.getLowFeedItems();
        return ResponseEntity.ok(new ApiResponse<>(true, "Low feed items fetched", watchlist));
    }


    // DELETE - delete an inventory item by ID
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String deletedBy = auth != null ? auth.getName() : "SYSTEM";
        service.delete(id, deletedBy);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Inventory record deleted successfully", null)
        );
    }

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
                new ApiResponse<>(true, "Inventory record deleted permanently", null)
        );
    }

    // RETORE
    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> restore(@PathVariable Long id) {
        tenantPlanGuardService.requireCurrentTenantPlanAccess(
                TenantPlan.PRO,
                "Trash restore is available on the Pro plan."
        );
        service.restore(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Inventory record restored", null)
        );
    }


    // SUMMARY - dashboard, summary & analysis
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary() {
        Map<String, Object> summary = service.getSummary();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Inventory summary fetched", summary)
        );
    }

    @PostMapping("/{id}/adjust")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<InventoryResponseDto>> adjustStock(
            @PathVariable Long id,
            @Valid @RequestBody InventoryAdjustmentRequestDto request
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String updatedBy = auth != null ? auth.getName() : "SYSTEM";
        InventoryResponseDto response = service.adjustStockById(
                id,
                request.getQuantityChange(),
                request.getNote(),
                updatedBy
        );
        return ResponseEntity.ok(new ApiResponse<>(true, "Inventory stock adjusted successfully", response));
    }

    // GET /api/inventory/catalog/items?category=FEED
    @GetMapping("/catalog/items")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<List<String>>> getItemsByCategory(
            @RequestParam InventoryCategory category
    ) {
        List<String> items = com.kfarms.catalog.InventoryCatalog.itemsForCategory(category);
        return ResponseEntity.ok(new ApiResponse<>(true, "Catalog items fetched", items));
    }

    // GET /api/inventory/catalog/threshold?itemName=Fish Feed 1mm
    @GetMapping("/catalog/threshold")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<Integer>> getDefaultThreshold(
            @RequestParam String itemName,
            @RequestParam InventoryCategory category
    ) {
        Optional<String> canonicalOpt = com.kfarms.catalog.InventoryCatalog.getCanonicalName(category, itemName);
        if (canonicalOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Unknown feed item: " + itemName, 0)
            );
        }

        int threshold = com.kfarms.catalog.InventoryCatalog.getDefaultThreshold(canonicalOpt.get());
        return ResponseEntity.ok(new ApiResponse<>(true, "Default threshold fetched", threshold));
    }





}
