package com.kfarms.controller;

import com.kfarms.dto.SalesRequestDto;
import com.kfarms.dto.SalesResponseDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.service.SalesService;
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
@RequestMapping("/api/sales")
public class SalesController {
    private final SalesService service;
    public SalesController(SalesService service){
        this.service = service;
    }

    // CREATE - add new supply item
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<SalesResponseDto>> create(
            @Valid @RequestBody SalesRequestDto dto){
        SalesResponseDto saved = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        new ApiResponse<>(true, "Sales record saved successfully", saved)
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            ){
        Map<String, Object> response = service.getAll(page, size, itemName, category, date);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "All sales record fetched successfully", response)
        );
    }

    // READ - get supply by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<SalesResponseDto>> getById(@PathVariable Long id){
        SalesResponseDto dto = service.getById(id);
        if(dto != null){
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Sales record fetched successfully", dto)
            );
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Sales with ID: " + id + " not found", null));
        }
    }

    // UPDATE - update existing sales item by ID
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<SalesResponseDto>> update(
            @PathVariable Long id,
            @RequestBody SalesRequestDto request
    ){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String updatedBy = auth.getName();
        SalesResponseDto response = service.update(id, request, updatedBy);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Sales updated successfully", response)
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
                new ApiResponse<>(true, "Sales record deleted successfully", null)
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

    // SUMMARY - dashboard, summary & analysis
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary() {
        Map<String, Object> summary = service.getSummary();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Sales summary fetched", summary)
        );
    }
}
