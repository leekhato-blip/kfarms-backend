package com.kfarms.platform.controller;

import com.kfarms.entity.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/platform/support")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformSupportController {

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTickets() {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Platform support tickets fetched successfully",
                        Map.of(
                                "items", List.of(),
                                "statusCounts", Map.of(),
                                "laneCounts", Map.of()
                        )
                )
        );
    }
}
