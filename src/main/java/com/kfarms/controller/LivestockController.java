package com.kfarms.controller;

import com.kfarms.dto.LivestockRequest;
import com.kfarms.dto.LivestockResponse;
import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.Livestock;
import com.kfarms.mapper.LivestockMapper;
import com.kfarms.service.LivestockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/livestock")
@RequiredArgsConstructor
public class LivestockController {
    private final LivestockService service;

    @PostMapping
    public ResponseEntity<ApiResponse<LivestockResponse>> save(
            @RequestBody LivestockRequest request,
            @RequestHeader(value = "X-user", defaultValue = "System") String createdBy
    ){
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Livestock saved successfully", request)
        );
    }
}
