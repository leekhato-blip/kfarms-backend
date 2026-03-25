package com.kfarms.platform.controller;

import com.kfarms.dto.SupportTicketMessageRequestDto;
import com.kfarms.dto.SupportTicketStatusUpdateRequestDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.service.SupportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/platform/support")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformSupportController {

    private final SupportService supportService;

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listTickets(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String lane,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Platform support tickets fetched successfully",
                        supportService.getPlatformTickets(search, status, lane, page, size)
                )
        );
    }

    @PostMapping("/tickets/{ticketId}/messages")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addReply(
            @PathVariable String ticketId,
            @Valid @RequestBody SupportTicketMessageRequestDto request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "PLATFORM";
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Platform support reply added successfully",
                        supportService.addPlatformTicketReply(ticketId, request, actor)
                )
        );
    }

    @PatchMapping("/tickets/{ticketId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateStatus(
            @PathVariable String ticketId,
            @Valid @RequestBody SupportTicketStatusUpdateRequestDto request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "PLATFORM";
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Platform support ticket updated successfully",
                        supportService.updatePlatformTicketStatus(ticketId, request, actor)
                )
        );
    }
}
