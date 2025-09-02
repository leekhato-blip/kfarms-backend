package com.kfarms.controller;

import com.fasterxml.jackson.core.ObjectCodec;
import com.kfarms.dto.LivestockRequest;
import com.kfarms.dto.LivestockResponse;
import com.kfarms.entity.ApiResponse;
import com.kfarms.service.LivestockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.Authenticator;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
            @Valid @RequestBody LivestockRequest request
    ){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String createdBy = auth.getName();

        LivestockResponse response = service.create(request, createdBy);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Livestock saved successfully", response)
        );
    }

    // READ - all livestock
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPERVISOR')")
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

//    public ResponseEntity<ApiResponse<List<LivestockResponse>>> getAll(){
//        List<LivestockResponse> responses = service.getAll();
//        return ResponseEntity.ok(
//                new ApiResponse<>(true, "All livestock fetched successfully", responses)
//        );
//    }

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
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Livestock with ID: " + id + " not found", null));
        }
    }

    // UPDATE - update existing livestock
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LivestockResponse>> update(
            @PathVariable Long id,
            @RequestBody LivestockRequest request
    ){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String updatedBy = auth.getName();
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
                new ApiResponse<>(true, "Livestock with ID: " + id + " deleted successfully", null
                ));
    }

    // SEARCH / FILTER
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') or hasRole('SUPERVISOR')")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<LivestockResponse>>> search(
            @RequestParam(required = false) String batchName,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrivalDate
    ){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Logged in as: " + auth.getName());
        System.out.println("Authorities: " + auth.getAuthorities());
        List<LivestockResponse> results = service.search(batchName, type, arrivalDate);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Search results fetched successfully", results)
        );
    }

    // DASHBOARD SUMMARY
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') or hasRole('SUPERVISOR')")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary(){
        Map<String, Object> summary = service.getSummary();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Livestock summary fetched successfully", summary)
        );
    }
}
