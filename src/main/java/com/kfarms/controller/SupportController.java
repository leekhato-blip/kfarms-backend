package com.kfarms.controller;

import com.kfarms.dto.SupportAssistantChatRequestDto;
import com.kfarms.dto.SupportTicketCreateRequestDto;
import com.kfarms.dto.SupportTicketMessageRequestDto;
import com.kfarms.dto.SupportTicketStatusUpdateRequestDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.service.SupportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    @GetMapping("/resources")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getResources() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Support resources fetched successfully", supportService.getResources())
        );
    }

    @GetMapping("/tickets")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTickets() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Support tickets fetched successfully", supportService.getTickets())
        );
    }

    @PostMapping("/tickets")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTicket(
            @Valid @RequestBody SupportTicketCreateRequestDto request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "SYSTEM";
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(true, "Support ticket created successfully", supportService.createTicket(request, actor))
        );
    }

    @PostMapping("/tickets/{ticketId}/messages")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addTicketReply(
            @PathVariable String ticketId,
            @Valid @RequestBody SupportTicketMessageRequestDto request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "SYSTEM";
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Support ticket reply added successfully", supportService.addTicketReply(ticketId, request, actor))
        );
    }

    @PatchMapping("/tickets/{ticketId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateTicketStatus(
            @PathVariable String ticketId,
            @Valid @RequestBody SupportTicketStatusUpdateRequestDto request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "SYSTEM";
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Support ticket status updated successfully", supportService.updateTicketStatus(ticketId, request, actor))
        );
    }

    @GetMapping("/chat/conversation")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAssistantConversation(Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "SYSTEM";
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Support assistant conversation fetched successfully", supportService.getAssistantConversation(actor))
        );
    }

    @DeleteMapping("/chat/conversation")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetAssistantConversation(Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "SYSTEM";
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Support assistant conversation reset successfully", supportService.resetAssistantConversation(actor))
        );
    }

    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chat(
            @Valid @RequestBody SupportAssistantChatRequestDto request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "SYSTEM";
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Support assistant reply generated successfully", supportService.chat(request, actor))
        );
    }
}
