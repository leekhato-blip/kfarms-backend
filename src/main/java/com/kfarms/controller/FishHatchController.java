package com.kfarms.controller;

import com.kfarms.dto.FishHatchRequestDto;
import com.kfarms.dto.FishHatchResponseDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.service.FishHatchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// TODO: Add role-based access control to this controller
@RestController
@RequestMapping("/api/fish-hatch")
public class FishHatchController {
    private final FishHatchService service;
    public FishHatchController(FishHatchService service){
        this.service = service;
    }

    // CREATE - create a new fishHatc record
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FishHatchResponseDto>> create(
            @Valid @RequestBody FishHatchRequestDto request){
        FishHatchResponseDto dto = service.create(request);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Fish hatch record created", dto)
        );
    }

    // READ - fetch all fish hatch records
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<List<FishHatchResponseDto>>> getAll(){
        List<FishHatchResponseDto>  list = service.getAll();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "All hatch records fetched", list)
        );
    }

    // READ - get existing fish hatch by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<FishHatchResponseDto>> getById(@PathVariable Long id) {
        FishHatchResponseDto dto = service.getById(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Fish hatch record fetched", dto)
        );
    }

    // UPDATE - update existing fishPond by ID
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FishHatchResponseDto>> update(
            @PathVariable Long id,
            @RequestBody FishHatchRequestDto request
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String updatedBy = auth != null ? auth.getName() : "SYSTEM";
        FishHatchResponseDto dto = service.update(id, request, updatedBy);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Fish hatch record updated", dto)
        );
    }

    // DELETE - delete existing fish hatch record
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String deletedBy = auth != null ? auth.getName() : "SYSTEM";
        service.delete(id, deletedBy);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "FishHatch record deleted successfully", null)
        );
    }

    // RESTORE
    // DELETE - delete existing fish hatch record
    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> restore(@PathVariable Long id) {
        service.restore(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Fish hatch record restored", null)
        );
    }

    // SUMMARY - analysis and reports
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary() {
        Map<String, Object> summary = service.getSummary();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Fish hatch summary fetched", summary)
        );
    }

}
