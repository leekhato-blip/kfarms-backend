package com.kfarms.platform.controller;


import com.kfarms.entity.ApiResponse;
import com.kfarms.platform.dto.PlatformUserListItemDto;
import com.kfarms.platform.service.PlatformUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformUserController {

    private final PlatformUserService platformUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PlatformUserListItemDto>>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<PlatformUserListItemDto> result =
                platformUserService.searchUsers(search, pageable);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Platform users fetched successfully",
                        result
                )
        );
    }

    @PatchMapping("{userId}/platform-admin")
    public ResponseEntity<ApiResponse<Void>> setPlatformAdmin(
            @PathVariable Long userId,
            @RequestParam boolean value
    ) {

        platformUserService.setPlatformAdmin(userId, value);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        value ? "User promoted to platform admin"
                                : "Platform admin role removed",
                        null
                )
        );
    }

    @PatchMapping("/{userId}/enabled")
    public ResponseEntity<ApiResponse<Void>> setUserEnabled(
            @PathVariable Long userId,
            @RequestParam boolean value
    ) {

        platformUserService.setUserEnabled(userId, value);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        value ? "User enabled successfully"
                                : "User disabled successfully",
                        null
                )
        );
    }
}
