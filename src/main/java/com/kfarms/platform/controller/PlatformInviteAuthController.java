package com.kfarms.platform.controller;

import com.kfarms.dto.LoginResponse;
import com.kfarms.entity.ApiResponse;
import com.kfarms.platform.dto.AcceptPlatformInviteRequest;
import com.kfarms.platform.dto.PlatformInvitePreviewDto;
import com.kfarms.platform.service.PlatformUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/platform-invites")
@RequiredArgsConstructor
public class PlatformInviteAuthController {

    private final PlatformUserService platformUserService;

    @GetMapping("/resolve")
    public ResponseEntity<ApiResponse<PlatformInvitePreviewDto>> resolveInvite(
            @RequestParam String token
    ) {
        PlatformInvitePreviewDto invitation = platformUserService.resolvePlatformInvite(token);
        return ResponseEntity.ok(new ApiResponse<>(true, "Platform invite loaded", invitation));
    }

    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<LoginResponse>> acceptInvite(
            @Valid @RequestBody AcceptPlatformInviteRequest request
    ) {
        LoginResponse response = platformUserService.acceptPlatformInvite(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Platform access ready", response));
    }
}
