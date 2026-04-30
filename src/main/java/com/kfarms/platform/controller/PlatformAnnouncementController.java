package com.kfarms.platform.controller;

import com.kfarms.entity.ApiResponse;
import com.kfarms.platform.dto.PlatformAnnouncementRequest;
import com.kfarms.platform.service.PlatformAnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/platform/announcements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformAnnouncementController {

    private final PlatformAnnouncementService platformAnnouncementService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendAnnouncement(
            @Valid @RequestBody PlatformAnnouncementRequest request
    ) {
        Map<String, Object> payload = platformAnnouncementService.sendAnnouncement(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Announcement sent successfully",
                        payload
                )
        );
    }
}
