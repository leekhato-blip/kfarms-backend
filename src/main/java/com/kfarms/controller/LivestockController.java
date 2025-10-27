package com.kfarms.controller;


import com.kfarms.dto.LivestockRequestDto;
import com.kfarms.dto.LivestockResponseDto;
import com.kfarms.dto.StockAdjustmentRequestDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.service.LivestockService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/livestock")
@AllArgsConstructor
@Validated
public class LivestockController {
    private final LivestockService service;

    // CREATE - add new livestock
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<LivestockResponseDto>> create(
            @Valid @RequestBody LivestockRequestDto request
    ){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String createdBy = auth.getName();

        LivestockResponseDto response = service.create(request, createdBy);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Livestock saved successfully", response)
        );
    }

    // READ - all livestock (pagination and filtering)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String batchName,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrivalDate

    ){
        Map<String, Object> response = service.getAll(page, size, batchName, type, arrivalDate);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Livestock fetched successfully", response)
        );
    }

    // READ - get livestock by ID
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LivestockResponseDto>> getById(@PathVariable Long id){
        LivestockResponseDto response = service.getById(id);
        if(response != null){
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Livestock fetched", response)
            );
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Livestock with ID: " + id + " not found", null));
        }
    }

    // UPDATE - update existing livestock
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LivestockResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody LivestockRequestDto request
    ){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String updatedBy = auth.getName();
        LivestockResponseDto response = service.update(id, request, updatedBy);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Livestock updated successfully", response)
        );
    }

    // DELETE - delete livestock by ID
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String deletedBy = auth.getName();
        service.delete(id, deletedBy);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Livestock with ID: " + id + " soft deleted successfully", null)
        );
    }

    // RESTORE
    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> restore(@PathVariable Long id) {
        service.restore(id);
        return ResponseEntity.ok(
                new ApiResponse(true, "Livestock record restored", null)
        );
    }

    // DASHBOARD SUMMARY
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary(){
        Map<String, Object> summary = service.getSummary();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Livestock summary fetched successfully", summary)
        );
    }

    // Adjust stock
    @PostMapping("/{id}/adjust-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LivestockResponseDto>> adjustStock(
            @PathVariable Long id,
            @Valid @RequestBody StockAdjustmentRequestDto request
    ){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String updatedBy = auth != null ? auth.getName() : "SYSTEM";
        LivestockResponseDto response = service.adjustStock(id, request, updatedBy);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Stock adjusted successfully", response)
        );
    }
}
