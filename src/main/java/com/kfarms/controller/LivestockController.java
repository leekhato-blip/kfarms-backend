package com.kfarms.controller;

import com.kfarms.dto.LivestockRequest;
import com.kfarms.dto.LivestockResponse;
import com.kfarms.entity.ApiResponse;
import com.kfarms.service.LivestockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/livestock")
@RequiredArgsConstructor
@Validated
public class LivestockController {
    private final LivestockService service;

    // CREATE - add new livestock
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<LivestockResponse>> create(
            @Valid @RequestBody LivestockRequest request,
            @RequestHeader("X-USER") String createdBy
    ){
        LivestockResponse response = service.create(request, createdBy);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Livestock saved successfully", response)
        );
    }

    // READ - all livestock
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPERVISOR')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<LivestockResponse>>> getAll(){
        List<LivestockResponse> responses = service.getAll();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "All livestock fetched successfully", responses)
        );
    }

    // READ - get livestock by ID
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPERVISOR')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LivestockResponse>> getById(@PathVariable Long id){
        LivestockResponse response = service.getById(id);
        if(response != null){
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Livestock fetched", response)
            );
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // UPDATE - update existing livestock
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LivestockResponse>> update(
            @PathVariable Long id,
            @RequestBody LivestockRequest request,
            @RequestHeader("X-USER") String updatedBy
    ){
        LivestockResponse response = service.update(id, request, updatedBy);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Livestock updated successfully", response)
        );
    }

    // DELETE - delete livestock by ID
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Livestock with ID " + id + " deleted successfully", null
                ));
    }
}
