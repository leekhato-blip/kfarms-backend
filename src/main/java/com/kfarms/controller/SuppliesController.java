package com.kfarms.controller;


import com.kfarms.dto.SuppliesRequestDto;
import com.kfarms.dto.SuppliesResponseDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.AppUser;
import com.kfarms.service.SuppliesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/supplies")
@RequiredArgsConstructor
public class SuppliesController {
    private final SuppliesService service;

    // CREATE - add a new supply
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<SuppliesResponseDto>> create(
            @Valid @RequestBody SuppliesRequestDto dto){
        SuppliesResponseDto saved = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        new ApiResponse<>(true, "Supply record saved successfully", saved)
                );
    }

    // READ - Get All (with Pagination and Filtering)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String itemName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            ){
        Map<String, Object> response = service.getAll(page, size, itemName, category, date);
        return ResponseEntity.ok(new ApiResponse<>(true, "Supplies fetched successfully", response));
    }

    // READ - get supply by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<SuppliesResponseDto>> getById(@PathVariable Long id) {
        SuppliesResponseDto dto = service.getById(id);
        if (dto != null){
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Supply record fetched successfully", dto)
            );
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Supply with ID: " + id + " not found", null));
        }
    }

    // UPDATE - update existing supply item by ID
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<SuppliesResponseDto>> update(
            @PathVariable Long id,
            @RequestBody SuppliesRequestDto request
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String updatedBy = auth != null ? auth.getName() : "SYSTEM";
        SuppliesResponseDto response = service.update(id, request, updatedBy);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Supply updated successfully", response)
        );
    }

    // DELETE - delete a supply item by ID
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String deletedBy = auth != null ? auth.getName() : "SYSTEM";
        service.delete(id, deletedBy);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Supply record deleted successfully", null)
        );
    }

    // RESTORE
    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> restore(@PathVariable Long id) {
        service.restore(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Record restored", null)
        );
    }

    // SUMMARY - dashboard, reports and analysis
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary(Authentication auth) {
        Map<String, Object> summary = service.getSummary();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Supply summary fetched", summary)
        );
    }
}
