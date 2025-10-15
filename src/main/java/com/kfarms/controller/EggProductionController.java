package com.kfarms.controller;

import com.kfarms.dto.EggProductionRequestDto;
import com.kfarms.dto.EggProductionResponseDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.service.EggProductionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.PublicKey;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/eggs")
@RequiredArgsConstructor
@Validated
public class EggProductionController {
    private final EggProductionService eggService;

    // CREATE
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<EggProductionResponseDto>> create(
            @Valid @RequestBody EggProductionRequestDto request) {
        EggProductionResponseDto response = eggService.create(request);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Egg record saved successfully", response)
        );
    }

    // READ - all eggs with pagination and filter
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long livestockId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate collectionDate
            ) {
        Map<String, Object> response = eggService.getAll(page, size, livestockId, collectionDate);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "all egg records fetched successfully", response)
        );
    }

    // READ - by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<EggProductionResponseDto>> getById(@PathVariable Long id) {
        EggProductionResponseDto response = eggService.getById(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Egg record fetched successfully", response)
        );
    }

    // UPDATE
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<EggProductionResponseDto>> update(
            @PathVariable Long id,
            @RequestBody EggProductionRequestDto request
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String updatedBy = auth != null ? auth.getName() : "SYSTEM";

        EggProductionResponseDto response = eggService.update(id, request, updatedBy);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Egg record updated successfully", response)
        );
    }

    // DELETE
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String deletedBy = auth != null ? auth.getName() : "SYSTEM";
        eggService.delete(id, deletedBy);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Egg record with ID: " + id + " soft deleted successfully", null)
        );
    }

    // RESTORE
    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public  ResponseEntity<ApiResponse<String>> restore(@PathVariable Long id) {
        eggService.restore(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Record restored", null)
        );
    }

    // SUMMARY
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary() {
        Map<String, Object> summary = eggService.getSummary();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Egg summary fetched successfully", summary)
        );
    }





}
