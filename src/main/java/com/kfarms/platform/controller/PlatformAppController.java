package com.kfarms.platform.controller;

import com.kfarms.entity.ApiResponse;
import com.kfarms.platform.dto.PlatformAppPortfolioDto;
import com.kfarms.platform.service.PlatformAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/apps")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformAppController {

    private final PlatformAppService platformAppService;

    @GetMapping
    public ResponseEntity<ApiResponse<PlatformAppPortfolioDto>> getPortfolio() {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Platform app portfolio fetched successfully",
                        platformAppService.getPortfolio()
                )
        );
    }
}
