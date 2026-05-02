package com.kfarms.controller;

import com.kfarms.dto.BillingCheckoutRequestDto;
import com.kfarms.dto.BillingOverviewDto;
import com.kfarms.dto.BillingPortalRequestDto;
import com.kfarms.dto.BillingVerifyCheckoutRequestDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.service.BillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<BillingOverviewDto>> getOverview() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Billing overview fetched successfully", billingService.getOverview())
        );
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Billing invoices fetched successfully", billingService.getInvoices(page, size))
        );
    }

    @PostMapping("/checkout/session")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createCheckoutSession(
            @Valid @RequestBody BillingCheckoutRequestDto request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "SYSTEM";
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Checkout session created successfully",
                        billingService.createCheckoutSession(
                                request.getPlanId(),
                                request.getBillingInterval(),
                                request.getSuccessUrl(),
                                request.getCancelUrl(),
                                request.getCustomerEmail(),
                                actor
                        )
                )
        );
    }

    @PostMapping("/checkout/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyCheckout(
            @Valid @RequestBody BillingVerifyCheckoutRequestDto request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "SYSTEM";
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Checkout verified successfully",
                        billingService.verifyCheckoutSession(
                                request.getReference(),
                                request.getPlanId(),
                                request.getBillingInterval(),
                                actor
                        )
                )
        );
    }

    @PostMapping("/subscription/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelSubscription(Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "SYSTEM";
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Subscription cancellation scheduled successfully",
                        billingService.cancelSubscription(actor)
                )
        );
    }

    @PostMapping("/subscription/downgrade")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> downgradeToFree(Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "SYSTEM";
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Workspace downgraded to Free plan successfully",
                        billingService.downgradeToFreePlan(actor)
                )
        );
    }

    @PostMapping("/portal/session")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPortalSession(
            @RequestBody(required = false) BillingPortalRequestDto request
    ) {
        String returnUrl = request != null ? request.getReturnUrl() : null;
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Billing management session prepared",
                        billingService.createPortalSession(returnUrl)
                )
        );
    }

    @PostMapping("/paystack/webhook")
    public ResponseEntity<Map<String, Object>> handlePaystackWebhook(
            @RequestHeader(value = "x-paystack-signature", required = false) String signature,
            @RequestBody String payload
    ) {
        billingService.handleWebhook(signature, payload);
        return ResponseEntity.ok(Map.of("received", true));
    }

    @GetMapping("/invoices/{invoiceId}/receipt")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<byte[]> downloadInvoiceReceipt(@PathVariable Long invoiceId) {
        byte[] content = billingService.buildInvoiceReceipt(invoiceId);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + invoiceId + "-receipt.txt\"")
                .body(content);
    }
}
