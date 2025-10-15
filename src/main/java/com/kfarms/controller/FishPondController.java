package com.kfarms.controller;

import com.kfarms.dto.FishPondRequestDto;
import com.kfarms.dto.FishPondResponseDto;
import com.kfarms.dto.StockAdjustmentRequestDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.service.FishPondService;
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

// TODO: Add role-based access control to this controller
@RestController
@RequestMapping("/api/fishpond")
public class FishPondController {

    private final FishPondService service;
    public FishPondController(FishPondService service){
        this.service = service;
    }

    // CREATE - add a new fishPond
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FishPondResponseDto>> create(
            @Valid @RequestBody FishPondRequestDto request){
       FishPondResponseDto saved = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        new ApiResponse<>(true, "FishPond saved successfully", saved)
                );
    }

    /**
     * READ - Get all FishPonds with filtering + pagination
     * Filters: pondName, pondType, status, lastWaterChange
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String pondName,
            @RequestParam(required = false) String pondType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lastWaterChange
            ){
        Map<String, Object> response = service.getAll(page, size, pondName, pondType, status, lastWaterChange);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "All FishPond fetched successfully", response)
        );
    }

    // READ - get existing fishPond by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<FishPondResponseDto>> getById(@PathVariable Long id){
        FishPondResponseDto dto = service.getById(id);
        if(dto != null){
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Fishpond record fetched successfully", dto)
            );
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "FishPond with ID: " + id + " not found", null));
        }
    }

    // UPDATE - update existing fishPond by ID
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FishPondResponseDto>> update(@PathVariable Long id, @RequestBody FishPondRequestDto request){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String updatedBy = auth != null ? auth.getName() : "SYSTEM";
        FishPondResponseDto response = service.update(id, request, updatedBy);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "FishPond updated successfully", response)
        );
    }


    // DELETE - delete existing fishPond record by ID
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String deletedBy = auth != null ? auth.getName() : "SYSTEM";
        service.delete(id, deletedBy);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "FishPond record deleted successfully", null)
        );
    }

    // DELETE
    public ResponseEntity<ApiResponse<String>> restore(@PathVariable Long id) {
        service.restore(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Record restored", null)
        );
    }

    // SUMMARY - dashboard, report and analysis
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary() {
        Map<String, Object> summary = service.getSummary();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "FishPond summary fetched", summary)
        );
    }

    // Adjust stock
    @PostMapping("/{id}/adjust-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FishPondResponseDto>> adjustStock(
            @Valid
            @PathVariable Long id,
            @RequestBody StockAdjustmentRequestDto request
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String updatedBy = auth.getName();
        FishPondResponseDto response = service.adjustStock(id, request, updatedBy);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Stock adjusted successfully", response)
        );
    }
}