package com.kfarms.controller;


import com.kfarms.dto.FeedRequestDto;
import com.kfarms.dto.FeedResponseDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feeds")
public class FeedController {
    private final FeedService service;


    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<Page<FeedResponseDto>>> getAll(
            @RequestParam(required = false) String batchType,
            @RequestParam(required = false) String feedName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<FeedResponseDto> result = service.getAll(batchType, feedName, PageRequest.of(page, size));
        return ResponseEntity.ok(new ApiResponse<>(true, "Feeds fetched successfully", result));
    }

    // CREATE - add new feed
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FeedResponseDto>> create(@RequestBody FeedRequestDto dto) {
        FeedResponseDto saved = service.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Feed saved successfully", saved));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<FeedResponseDto>> getById(@PathVariable Long id) {
        FeedResponseDto dto = service.getById(id);
        if (dto != null) {
            return ResponseEntity.ok(new ApiResponse<>(true, "Feed detail fetched", dto));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Feed with ID: " + id + " not found", null));
        }
    }



    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "feed deleted successfully", null)
        );
    }

}
