package com.kfarms.service;

import com.kfarms.dto.BillingOverviewDto;

import java.util.Map;

public interface BillingService {
    BillingOverviewDto getOverview();
    Map<String, Object> getInvoices(int page, int size);
    Map<String, Object> createCheckoutSession(String planId, String billingInterval, String successUrl, String cancelUrl, String customerEmail, String actor);
    Map<String, Object> verifyCheckoutSession(String reference, String planId, String billingInterval, String actor);
    Map<String, Object> cancelSubscription(String actor);
    Map<String, Object> downgradeToFreePlan(String actor);
    Map<String, Object> createPortalSession(String returnUrl);
    Map<String, Object> handleWebhook(String signature, String payload);
    byte[] buildInvoiceReceipt(Long invoiceId);
}
