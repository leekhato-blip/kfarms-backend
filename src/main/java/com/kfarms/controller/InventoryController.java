package com.kfarms.controller;

import com.kfarms.dto.InventoryRequestDto;
import com.kfarms.dto.InventoryResponseDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.service.InventoryService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService service;
    public InventoryController(InventoryService service){
        this.service = service;
    }

    // CREATE - add new inventory item
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<InventoryResponseDto>> create(
            @Valid @RequestBody InventoryRequestDto dto) {
        InventoryResponseDto saved = service.create(dto);
        return ResponseEntity.ok(
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lastUpdated
            ){
        Map<String, Object> response = service.getAll(page, size, itemName, category, lastUpdated);
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
            @RequestBody InventoryRequestDto request
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String updatedBy = auth.getName();
        InventoryResponseDto response = service.update(id, request, updatedBy);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Inventory updated successfully", response)
        );

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

    // RETORE
    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> restore(@PathVariable Long id) {
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

}
