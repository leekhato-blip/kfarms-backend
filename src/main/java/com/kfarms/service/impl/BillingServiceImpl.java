package com.kfarms.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfarms.billing.PaystackClient;
import com.kfarms.dto.BillingInvoiceDto;
import com.kfarms.dto.BillingOverviewDto;
import com.kfarms.entity.*;
import com.kfarms.repository.BillingCheckoutSessionRepository;
import com.kfarms.repository.BillingInvoiceRepository;
import com.kfarms.repository.BillingSubscriptionRepository;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.entity.TenantPlan;
import com.kfarms.tenant.repository.TenantRepository;
import com.kfarms.service.BillingService;
import com.kfarms.tenant.service.TenantContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class BillingServiceImpl implements BillingService {

    private static final String PROVIDER_NAME = "PAYSTACK";
    private static final String FREE_PROVIDER = "NONE";
    private static final DateTimeFormatter RECEIPT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM uuuu HH:mm", Locale.ENGLISH);

    private final BillingSubscriptionRepository subscriptionRepository;
    private final BillingInvoiceRepository invoiceRepository;
    private final BillingCheckoutSessionRepository checkoutSessionRepository;
    private final TenantRepository tenantRepository;
    private final PaystackClient paystackClient;
    private final ObjectMapper objectMapper;

    @Override
    public BillingOverviewDto getOverview() {
        Tenant tenant = requireTenant();
        BillingSubscription subscription = prepareBillingState(tenant);
        return toOverviewDto(subscription, tenant);
    }

    @Override
    public Map<String, Object> getInvoices(int page, int size) {
        Tenant tenant = requireTenant();
        prepareBillingState(tenant);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );
        Page<BillingInvoice> invoicePage = invoiceRepository.findByTenant_Id(tenant.getId(), pageable);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", invoicePage.getContent().stream().map(this::toInvoiceDto).toList());
        result.put("page", invoicePage.getNumber());
        result.put("size", invoicePage.getSize());
        result.put("totalItems", invoicePage.getTotalElements());
        result.put("totalPages", Math.max(invoicePage.getTotalPages(), 1));
        result.put("hasNext", invoicePage.hasNext());
        result.put("hasPrevious", invoicePage.hasPrevious());
        return result;
    }

    @Override
    public Map<String, Object> createCheckoutSession(
            String planId,
            String successUrl,
            String cancelUrl,
            String customerEmail,
            String actor
    ) {
        Tenant tenant = requireTenant();
        TenantPlan requestedPlan = parseCheckoutPlan(planId);
        BillingSubscription subscription = prepareBillingState(tenant);
        String resolvedEmail = hasText(customerEmail)
                ? customerEmail.trim()
                : blankToNull(tenant.getContactEmail());

        if (!hasText(resolvedEmail)) {
            throw new IllegalArgumentException("A billing email is required before starting checkout.");
        }

        if (tenant.getPlan() == requestedPlan
                && subscription.getStatus() == BillingSubscriptionStatus.ACTIVE
                && !Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd())) {
            throw new IllegalArgumentException("This plan is already active for your workspace.");
        }

        BillingCheckoutSession session = new BillingCheckoutSession();
        session.setTenant(tenant);
        session.setPlan(requestedPlan);
        session.setProvider(PROVIDER_NAME);
        session.setReference(generateReference("CHK", tenant.getId()));
        session.setAmount(checkoutAmountForPlan(requestedPlan));
        session.setCurrency(currencyForPlan(requestedPlan));
        session.setCustomerEmail(resolvedEmail);
        session.setSuccessUrl(blankToNull(successUrl));
        session.setCancelUrl(blankToNull(cancelUrl));
        session.setStatus(BillingCheckoutStatus.PENDING);
        session.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        session.setCreatedBy(actor);
        session.setUpdatedBy(actor);
        checkoutSessionRepository.save(session);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tenantId", tenant.getId());
        metadata.put("tenantSlug", tenant.getSlug());
        metadata.put("planId", requestedPlan.name());
        metadata.put("actor", actor);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", resolvedEmail);
        payload.put("reference", session.getReference());
        payload.put("currency", currencyForPlan(requestedPlan));
        payload.put("callback_url", buildCheckoutUrl(successUrl, session.getReference(), requestedPlan));
        payload.put("metadata", metadata);

        // The checkout should reflect the temporary intro price. We only send the discounted
        // amount here, then attach the recurring Paystack plan after a successful authorization.
        payload.put("amount", toKobo(checkoutAmountForPlan(requestedPlan)));

        JsonNode providerData = paystackClient.initializeTransaction(payload);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkoutUrl", text(providerData, "authorization_url"));
        result.put("reference", session.getReference());
        result.put("provider", PROVIDER_NAME);
        result.put("amount", session.getAmount());
        result.put("currency", session.getCurrency());
        return result;
    }

    @Override
    public Map<String, Object> verifyCheckoutSession(String reference, String planId, String actor) {
        Tenant tenant = requireTenant();
        TenantPlan requestedPlan = parseCheckoutPlan(planId);
        BillingCheckoutSession session = checkoutSessionRepository.findByReferenceAndTenant_Id(reference, tenant.getId())
                .orElseThrow(() -> new IllegalArgumentException("Checkout session not found for this reference."));

        if (session.getStatus() == BillingCheckoutStatus.COMPLETED) {
            BillingSubscription current = prepareBillingState(tenant);
            BillingInvoice existingInvoice = invoiceRepository
                    .findFirstByTenant_IdAndReferenceOrderByIdDesc(tenant.getId(), reference)
                    .orElse(null);

            Map<String, Object> idempotent = new LinkedHashMap<>();
            idempotent.put("billing", toOverviewDto(current, tenant));
            idempotent.put("invoice", existingInvoice != null ? toInvoiceDto(existingInvoice) : null);
            return idempotent;
        }

        if (session.getStatus() != BillingCheckoutStatus.PENDING) {
            throw new IllegalArgumentException("This checkout session cannot be verified anymore.");
        }

        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(LocalDateTime.now())) {
            session.setStatus(BillingCheckoutStatus.EXPIRED);
            session.setUpdatedBy(actor);
            checkoutSessionRepository.save(session);
            throw new IllegalArgumentException("Checkout session has expired. Please start a new checkout.");
        }

        JsonNode paymentData = paystackClient.verifyTransaction(reference);
        TenantPlan resolvedPlan = session.getPlan() != null ? session.getPlan() : requestedPlan;
        return finalizeSuccessfulCheckout(tenant, session, resolvedPlan, paymentData, actor);
    }

    @Override
    public Map<String, Object> cancelSubscription(String actor) {
        Tenant tenant = requireTenant();
        BillingSubscription subscription = prepareBillingState(tenant);

        if (tenant.getPlan() == TenantPlan.FREE) {
            throw new IllegalArgumentException("Free plan does not have an active paid subscription to cancel.");
        }

        if (hasText(subscription.getProviderSubscriptionCode()) && hasText(subscription.getProviderSubscriptionToken())) {
            paystackClient.disableSubscription(
                    subscription.getProviderSubscriptionCode(),
                    subscription.getProviderSubscriptionToken()
            );
        }

        subscription.setStatus(BillingSubscriptionStatus.CANCELED);
        subscription.setCancelAtPeriodEnd(true);
        if (subscription.getNextBillingDate() == null && tenant.getPlan() == TenantPlan.PRO) {
            subscription.setNextBillingDate(LocalDate.now().plusDays(30));
        }
        subscription.setUpdatedBy(actor);
        subscription = subscriptionRepository.save(subscription);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("billing", toOverviewDto(subscription, tenant));
        return result;
    }

    @Override
    public Map<String, Object> downgradeToFreePlan(String actor) {
        Tenant tenant = requireTenant();
        tenant.setPlan(TenantPlan.FREE);
        tenant.setUpdatedBy(actor);
        tenantRepository.save(tenant);

        BillingSubscription subscription = subscriptionRepository.findByTenant_Id(tenant.getId())
                .orElseGet(() -> initializeSubscription(tenant));
        if (hasText(subscription.getProviderSubscriptionCode()) && hasText(subscription.getProviderSubscriptionToken())) {
            paystackClient.disableSubscription(
                    subscription.getProviderSubscriptionCode(),
                    subscription.getProviderSubscriptionToken()
            );
        }
        applyPlanState(subscription, TenantPlan.FREE);
        subscription.setUpdatedBy(actor);
        subscription = subscriptionRepository.save(subscription);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("billing", toOverviewDto(subscription, tenant));
        return result;
    }

    @Override
    public Map<String, Object> createPortalSession(String returnUrl) {
        Tenant tenant = requireTenant();
        BillingSubscription subscription = prepareBillingState(tenant);
        boolean paymentSettingsAvailable = hasText(subscription.getProviderSubscriptionCode());
        Map<String, Object> result = new LinkedHashMap<>();

        if (!paymentSettingsAvailable) {
            result.put("portalUrl", "");
            result.put("available", false);
            result.put("paymentSettingsAvailable", false);
            return result;
        }

        result.put("portalUrl", paystackClient.createSubscriptionManageLink(subscription.getProviderSubscriptionCode()));
        result.put("available", true);
        result.put("paymentSettingsAvailable", true);
        return result;
    }

    @Override
    public Map<String, Object> handleWebhook(String signature, String payload) {
        if (!paystackClient.verifyWebhookSignature(payload, signature)) {
            throw new AccessDeniedException("Invalid Paystack webhook signature.");
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String event = text(root, "event");
            JsonNode data = root.path("data");

            return switch (event) {
                case "charge.success" -> handleChargeSuccessWebhook(data);
                case "subscription.create" -> handleSubscriptionCreatedWebhook(data);
                case "subscription.disable" -> handleSubscriptionDisabledWebhook(data);
                case "invoice.payment_failed" -> handleInvoicePaymentFailedWebhook(data);
                default -> Map.of("handled", false, "event", event);
            };
        } catch (AccessDeniedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not process Paystack webhook payload.");
        }
    }

    @Override
    public byte[] buildInvoiceReceipt(Long invoiceId) {
        Tenant tenant = requireTenant();
        BillingInvoice invoice = invoiceRepository.findByIdAndTenant_Id(invoiceId, tenant.getId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found."));
        BillingSubscription subscription = prepareBillingState(tenant);

        StringBuilder receipt = new StringBuilder();
        receipt.append("KFARMS RECEIPT").append("\n");
        receipt.append("================").append("\n\n");
        receipt.append("Workspace: ").append(tenant.getName()).append("\n");
        receipt.append("Plan: ").append(Optional.ofNullable(subscription.getPlan()).orElse(TenantPlan.FREE)).append("\n");
        receipt.append("Invoice ID: ").append(invoice.getId()).append("\n");
        receipt.append("Reference: ").append(invoice.getReference()).append("\n");
        receipt.append("Status: ").append(invoice.getStatus()).append("\n");
        receipt.append("Date: ").append(formatDateTime(invoice.getCreatedAt())).append("\n");
        receipt.append("Paid At: ").append(formatDateTime(invoice.getPaidAt())).append("\n");
        receipt.append("Description: ").append(invoice.getDescription()).append("\n");
        receipt.append("Amount: ").append(invoice.getCurrency()).append(" ").append(invoice.getAmount()).append("\n");
        receipt.append("Provider: ").append(subscription.getProvider()).append("\n");
        receipt.append("Payment Method: ");
        if (hasText(subscription.getPaymentMethodBrand())) {
            receipt.append(subscription.getPaymentMethodBrand().toUpperCase(Locale.ROOT))
                    .append(" •••• ")
                    .append(Optional.ofNullable(subscription.getPaymentMethodLast4()).orElse("----"));
        } else {
            receipt.append("Not recorded");
        }
        receipt.append("\n");

        return receipt.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Tenant requireTenant() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Missing tenant context");
        }
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found."));
    }

    private BillingSubscription prepareBillingState(Tenant tenant) {
        BillingSubscription subscription = subscriptionRepository.findByTenant_Id(tenant.getId())
                .orElseGet(() -> initializeSubscription(tenant));

        boolean changed = syncWithTenantPlan(subscription, tenant);
        changed |= applyBillingCycleRules(subscription, tenant);

        if (changed) {
            subscription = subscriptionRepository.save(subscription);
            tenantRepository.save(tenant);
        }

        return subscription;
    }

    private BillingSubscription initializeSubscription(Tenant tenant) {
        BillingSubscription subscription = new BillingSubscription();
        subscription.setTenant(tenant);
        applyPlanState(subscription, tenant.getPlan());
        if (tenant.getPlan() == TenantPlan.PRO) {
            subscription.setSubscriptionReference(generateReference("SUB", tenant.getId()));
        }
        return subscriptionRepository.save(subscription);
    }

    private boolean syncWithTenantPlan(BillingSubscription subscription, Tenant tenant) {
        TenantPlan plan = Optional.ofNullable(tenant.getPlan()).orElse(TenantPlan.FREE);
        if (subscription.getPlan() == plan) {
            return false;
        }
        applyPlanState(subscription, plan);
        if (plan == TenantPlan.PRO && !hasText(subscription.getSubscriptionReference())) {
            subscription.setSubscriptionReference(generateReference("SUB", tenant.getId()));
        }
        return true;
    }

    private boolean applyBillingCycleRules(BillingSubscription subscription, Tenant tenant) {
        if (subscription.getPlan() != TenantPlan.PRO) {
            return false;
        }
        if (PROVIDER_NAME.equalsIgnoreCase(subscription.getProvider())) {
            return false;
        }
        if (subscription.getNextBillingDate() == null) {
            return false;
        }
        if (subscription.getNextBillingDate().isAfter(LocalDate.now())) {
            return false;
        }

        if (Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd())) {
            tenant.setPlan(TenantPlan.FREE);
            applyPlanState(subscription, TenantPlan.FREE);
            return true;
        }

        createInvoice(
                tenant,
                subscription,
                "PRO subscription renewal",
                amountForPlan(TenantPlan.PRO),
                currencyForPlan(TenantPlan.PRO),
                generateReference("INV", tenant.getId()),
                "SYSTEM"
        );
        subscription.setStatus(BillingSubscriptionStatus.ACTIVE);
        subscription.setNextBillingDate(LocalDate.now().plusDays(30));
        subscription.setUpdatedBy("SYSTEM");
        return true;
    }

    private void applyPlanState(BillingSubscription subscription, TenantPlan plan) {
        subscription.setPlan(plan);
        subscription.setAmount(amountForPlan(plan));
        subscription.setCurrency(currencyForPlan(plan));
        subscription.setBillingInterval(intervalForPlan(plan));

        if (plan == TenantPlan.FREE) {
            subscription.setStatus(BillingSubscriptionStatus.ACTIVE);
            subscription.setProvider(FREE_PROVIDER);
            subscription.setNextBillingDate(null);
            subscription.setCancelAtPeriodEnd(false);
            subscription.setSubscriptionReference(blankToNull(subscription.getSubscriptionReference()));
            subscription.setProviderPlanCode(null);
            subscription.setProviderSubscriptionCode(null);
            subscription.setProviderSubscriptionToken(null);
            return;
        }

        subscription.setStatus(BillingSubscriptionStatus.ACTIVE);
        subscription.setProvider(PROVIDER_NAME);
        subscription.setCancelAtPeriodEnd(false);
        if (plan == TenantPlan.PRO && subscription.getNextBillingDate() == null) {
            subscription.setNextBillingDate(LocalDate.now().plusDays(30));
        }
        if (plan == TenantPlan.ENTERPRISE) {
            subscription.setNextBillingDate(null);
        }
    }

    private BillingInvoice createInvoice(
            Tenant tenant,
            BillingSubscription subscription,
            String description,
            BigDecimal amount,
            String currency,
            String reference,
            String actor
    ) {
        BillingInvoice invoice = new BillingInvoice();
        invoice.setTenant(tenant);
        invoice.setSubscription(subscription);
        invoice.setDescription(description);
        invoice.setAmount(amount);
        invoice.setCurrency(currency);
        invoice.setStatus(BillingInvoiceStatus.PAID);
        invoice.setReference(reference);
        invoice.setPaidAt(LocalDateTime.now());
        invoice.setCreatedBy(actor);
        invoice.setUpdatedBy(actor);
        return invoiceRepository.save(invoice);
    }

    private BillingOverviewDto toOverviewDto(BillingSubscription subscription, Tenant tenant) {
        BillingOverviewDto dto = new BillingOverviewDto();
        dto.setPlanId(
                Optional.ofNullable(subscription.getPlan())
                        .orElse(Optional.ofNullable(tenant.getPlan()).orElse(TenantPlan.FREE))
                        .name()
        );
        dto.setStatus(Optional.ofNullable(subscription.getStatus()).orElse(BillingSubscriptionStatus.ACTIVE).name());
        dto.setAmount(subscription.getAmount());
        dto.setCurrency(subscription.getCurrency());
        dto.setInterval(subscription.getBillingInterval());
        dto.setProvider(subscription.getProvider());
        dto.setNextBillingDate(subscription.getNextBillingDate());
        dto.setCancelAtPeriodEnd(subscription.getCancelAtPeriodEnd());
        dto.setSubscriptionReference(subscription.getSubscriptionReference());
        dto.setPaymentMethodBrand(subscription.getPaymentMethodBrand());
        dto.setPaymentMethodLast4(subscription.getPaymentMethodLast4());
        dto.setPaymentSettingsAvailable(hasText(subscription.getProviderSubscriptionCode()));
        dto.setUpdatedAt(
                subscription.getUpdatedAt() != null
                        ? subscription.getUpdatedAt()
                        : subscription.getCreatedAt()
        );
        return dto;
    }

    private BillingInvoiceDto toInvoiceDto(BillingInvoice invoice) {
        BillingInvoiceDto dto = new BillingInvoiceDto();
        dto.setId(String.valueOf(invoice.getId()));
        dto.setCreatedAt(invoice.getCreatedAt() != null ? invoice.getCreatedAt() : invoice.getPaidAt());
        dto.setDescription(invoice.getDescription());
        dto.setAmount(invoice.getAmount());
        dto.setCurrency(invoice.getCurrency());
        dto.setStatus(invoice.getStatus().name());
        dto.setReference(invoice.getReference());
        dto.setDownloadUrl("/api/billing/invoices/" + invoice.getId() + "/receipt");
        return dto;
    }

    private Map<String, Object> finalizeSuccessfulCheckout(
            Tenant tenant,
            BillingCheckoutSession session,
            TenantPlan resolvedPlan,
            JsonNode paymentData,
            String actor
    ) {
        String paymentStatus = text(paymentData, "status");
        if (!"success".equalsIgnoreCase(paymentStatus)) {
            throw new IllegalArgumentException("Payment was not completed successfully.");
        }

        BigDecimal paidAmount = amountFromKobo(paymentData.path("amount"));
        BigDecimal expectedAmount = Optional.ofNullable(session.getAmount())
                .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                .orElseGet(() -> checkoutAmountForPlan(resolvedPlan));
        if (expectedAmount.compareTo(BigDecimal.ZERO) > 0 && paidAmount.compareTo(expectedAmount) < 0) {
            throw new IllegalArgumentException("Verified payment amount is lower than the selected plan amount.");
        }

        tenant.setPlan(resolvedPlan);
        tenant.setUpdatedBy(actor);
        tenantRepository.save(tenant);

        BillingSubscription subscription = subscriptionRepository.findByTenant_Id(tenant.getId())
                .orElseGet(() -> initializeSubscription(tenant));
        applyPlanState(subscription, resolvedPlan);
        updateSubscriptionFromPaymentData(subscription, resolvedPlan, paymentData, actor);
        subscription = subscriptionRepository.save(subscription);

        String reference = text(paymentData, "reference");
        BillingInvoice invoice = invoiceRepository.findFirstByTenant_IdAndReferenceOrderByIdDesc(tenant.getId(), reference)
                .orElse(null);
        if (invoice == null) {
            invoice = createInvoice(
                    tenant,
                    subscription,
                    resolvedPlan.name() + " subscription payment",
                    paidAmount.compareTo(BigDecimal.ZERO) > 0 ? paidAmount : expectedAmount,
                    firstNonBlank(text(paymentData, "currency"), currencyForPlan(resolvedPlan)),
                    reference,
                    actor
            );
        }

        session.setStatus(BillingCheckoutStatus.COMPLETED);
        session.setVerifiedAt(LocalDateTime.now());
        session.setUpdatedBy(actor);
        checkoutSessionRepository.save(session);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("billing", toOverviewDto(subscription, tenant));
        result.put("invoice", toInvoiceDto(invoice));
        return result;
    }

    private void updateSubscriptionFromPaymentData(
            BillingSubscription subscription,
            TenantPlan plan,
            JsonNode paymentData,
            String actor
    ) {
        subscription.setStatus(BillingSubscriptionStatus.ACTIVE);
        subscription.setProvider(PROVIDER_NAME);
        subscription.setCancelAtPeriodEnd(false);
        subscription.setNextBillingDate(plan == TenantPlan.PRO ? LocalDate.now().plusDays(30) : null);
        subscription.setUpdatedBy(actor);
        BigDecimal paidAmount = amountFromKobo(paymentData.path("amount"));
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            subscription.setAmount(paidAmount);
        }

        JsonNode customer = paymentData.path("customer");
        JsonNode authorization = paymentData.path("authorization");
        JsonNode planObject = paymentData.path("plan_object");

        String customerCode = text(customer, "customer_code");
        String authorizationCode = text(authorization, "authorization_code");
        String providerPlanCode = firstNonBlank(
                text(planObject, "plan_code"),
                text(paymentData, "plan"),
                resolveProviderPlanCode(plan)
        );

        subscription.setProviderCustomerCode(blankToNull(customerCode));
        subscription.setProviderPlanCode(blankToNull(providerPlanCode));
        subscription.setAuthorizationCode(blankToNull(authorizationCode));
        subscription.setSubscriptionReference(
                blankToNull(
                        firstNonBlank(
                                subscription.getProviderSubscriptionCode(),
                                text(paymentData, "reference"),
                                subscription.getSubscriptionReference(),
                                generateReference("SUB", subscription.getTenant().getId())
                        )
                )
        );
        subscription.setPaymentMethodBrand(
                blankToNull(
                        firstNonBlank(
                                text(authorization, "brand"),
                                text(authorization, "card_type"),
                                text(paymentData, "channel")
                        )
                )
        );
        subscription.setPaymentMethodLast4(blankToNull(text(authorization, "last4")));
        subscription.setPaymentChannel(blankToNull(firstNonBlank(text(paymentData, "channel"), text(authorization, "channel"))));

        createProviderSubscriptionIfNeeded(subscription, providerPlanCode, customerCode, authorizationCode);
    }

    private void createProviderSubscriptionIfNeeded(
            BillingSubscription subscription,
            String providerPlanCode,
            String customerCode,
            String authorizationCode
    ) {
        if (subscription.getPlan() != TenantPlan.PRO) {
            return;
        }
        if (hasText(subscription.getProviderSubscriptionCode())) {
            return;
        }
        if (!hasText(providerPlanCode) || !hasText(customerCode) || !hasText(authorizationCode)) {
            return;
        }

        JsonNode subscriptionData = paystackClient.createSubscription(customerCode, providerPlanCode, authorizationCode);
        String subscriptionCode = text(subscriptionData, "subscription_code");
        String subscriptionToken = text(subscriptionData, "email_token");

        subscription.setProviderSubscriptionCode(blankToNull(subscriptionCode));
        subscription.setProviderSubscriptionToken(blankToNull(subscriptionToken));
        subscription.setSubscriptionReference(blankToNull(firstNonBlank(subscriptionCode, subscription.getSubscriptionReference())));
    }

    private Map<String, Object> handleChargeSuccessWebhook(JsonNode data) {
        String reference = text(data, "reference");
        Optional<BillingCheckoutSession> checkoutSession = checkoutSessionRepository.findByReference(reference);
        if (checkoutSession.isPresent()) {
            BillingCheckoutSession session = checkoutSession.get();
            return finalizeSuccessfulCheckout(
                    session.getTenant(),
                    session,
                    Optional.ofNullable(session.getPlan()).orElse(TenantPlan.PRO),
                    data,
                    "PAYSTACK_WEBHOOK"
            );
        }

        BillingSubscription subscription = findProviderSubscription(data).orElse(null);
        if (subscription == null) {
            return Map.of("handled", false, "event", "charge.success", "reference", reference);
        }

        updateSubscriptionFromPaymentData(subscription, subscription.getPlan(), data, "PAYSTACK_WEBHOOK");
        subscription = subscriptionRepository.save(subscription);

        BillingInvoice invoice = invoiceRepository.findFirstByTenant_IdAndReferenceOrderByIdDesc(
                        subscription.getTenant().getId(),
                        reference
                )
                .orElse(null);
        if (invoice == null) {
            invoice = createInvoice(
                    subscription.getTenant(),
                    subscription,
                    subscription.getPlan().name() + " subscription renewal",
                    amountFromKobo(data.path("amount")),
                    firstNonBlank(text(data, "currency"), subscription.getCurrency(), "NGN"),
                    reference,
                    "PAYSTACK_WEBHOOK"
            );
        }

        return Map.of(
                "handled", true,
                "event", "charge.success",
                "invoiceId", invoice.getId()
        );
    }

    private Map<String, Object> handleSubscriptionCreatedWebhook(JsonNode data) {
        BillingSubscription subscription = findProviderSubscription(data).orElse(null);
        if (subscription == null) {
            return Map.of("handled", false, "event", "subscription.create");
        }

        subscription.setProviderSubscriptionCode(blankToNull(firstNonBlank(
                text(data, "subscription_code"),
                text(data.path("subscription"), "subscription_code"),
                subscription.getProviderSubscriptionCode()
        )));
        subscription.setProviderSubscriptionToken(blankToNull(firstNonBlank(
                text(data, "email_token"),
                text(data.path("subscription"), "email_token"),
                subscription.getProviderSubscriptionToken()
        )));
        subscription.setProviderPlanCode(blankToNull(firstNonBlank(
                text(data.path("plan"), "plan_code"),
                subscription.getProviderPlanCode()
        )));
        subscription.setStatus(BillingSubscriptionStatus.ACTIVE);
        subscription.setCancelAtPeriodEnd(false);
        subscription.setUpdatedBy("PAYSTACK_WEBHOOK");
        subscriptionRepository.save(subscription);

        return Map.of("handled", true, "event", "subscription.create");
    }

    private Map<String, Object> handleSubscriptionDisabledWebhook(JsonNode data) {
        BillingSubscription subscription = findProviderSubscription(data).orElse(null);
        if (subscription == null) {
            return Map.of("handled", false, "event", "subscription.disable");
        }

        subscription.setStatus(BillingSubscriptionStatus.CANCELED);
        subscription.setCancelAtPeriodEnd(true);
        subscription.setUpdatedBy("PAYSTACK_WEBHOOK");
        subscriptionRepository.save(subscription);

        return Map.of("handled", true, "event", "subscription.disable");
    }

    private Map<String, Object> handleInvoicePaymentFailedWebhook(JsonNode data) {
        BillingSubscription subscription = findProviderSubscription(data).orElse(null);
        if (subscription == null) {
            return Map.of("handled", false, "event", "invoice.payment_failed");
        }

        subscription.setStatus(BillingSubscriptionStatus.FAILED);
        subscription.setUpdatedBy("PAYSTACK_WEBHOOK");
        subscriptionRepository.save(subscription);

        return Map.of("handled", true, "event", "invoice.payment_failed");
    }

    private Optional<BillingSubscription> findProviderSubscription(JsonNode data) {
        String subscriptionCode = firstNonBlank(
                text(data, "subscription_code"),
                text(data.path("subscription"), "subscription_code")
        );
        if (hasText(subscriptionCode)) {
            Optional<BillingSubscription> bySubscriptionCode =
                    subscriptionRepository.findByProviderSubscriptionCode(subscriptionCode);
            if (bySubscriptionCode.isPresent()) {
                return bySubscriptionCode;
            }
        }

        String customerCode = text(data.path("customer"), "customer_code");
        if (hasText(customerCode)) {
            return subscriptionRepository.findByProviderCustomerCode(customerCode);
        }

        return Optional.empty();
    }

    private TenantPlan parseCheckoutPlan(String planId) {
        TenantPlan plan = parsePlan(planId);
        if (plan == TenantPlan.FREE) {
            throw new IllegalArgumentException("Use the downgrade action to switch to the Free plan.");
        }
        if (plan == TenantPlan.ENTERPRISE) {
            throw new IllegalArgumentException("Enterprise plans are handled through direct sales.");
        }
        return plan;
    }

    private TenantPlan parsePlan(String planId) {
        if (planId == null || planId.isBlank()) {
            throw new IllegalArgumentException("Plan ID is required.");
        }
        try {
            return TenantPlan.valueOf(planId.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported plan: " + planId);
        }
    }

    private BigDecimal amountForPlan(TenantPlan plan) {
        return switch (plan) {
            case PRO -> BigDecimal.valueOf(10_000);
            case FREE, ENTERPRISE -> BigDecimal.ZERO;
        };
    }

    private BigDecimal checkoutAmountForPlan(TenantPlan plan) {
        return switch (plan) {
            // The first month is temporarily discounted, while renewals continue on the
            // regular Paystack recurring plan configured for PRO.
            case PRO -> BigDecimal.valueOf(7_000);
            case FREE, ENTERPRISE -> BigDecimal.ZERO;
        };
    }

    private String currencyForPlan(TenantPlan plan) {
        return "NGN";
    }

    private String intervalForPlan(TenantPlan plan) {
        return switch (plan) {
            case PRO, FREE -> "MONTHLY";
            case ENTERPRISE -> "CONTRACT";
        };
    }

    private String resolveProviderPlanCode(TenantPlan plan) {
        if (plan != TenantPlan.PRO) {
            return "";
        }
        return paystackClient.getProMonthlyPlanCode();
    }

    private int toKobo(BigDecimal amount) {
        return amount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();
    }

    private BigDecimal amountFromKobo(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return BigDecimal.ZERO;
        }

        BigDecimal kobo = node.decimalValue();
        return kobo.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private String buildCheckoutUrl(String successUrl, String reference, TenantPlan plan) {
        String base = hasText(successUrl) ? successUrl.trim() : "/billing";
        return UriComponentsBuilder.fromUriString(base)
                .queryParam("paymentStatus", "success")
                .queryParam("reference", reference)
                .queryParam("plan", plan.name())
                .queryParam("provider", PROVIDER_NAME)
                .build(true)
                .toUriString();
    }

    private String buildPortalUrl(String returnUrl) {
        String base = hasText(returnUrl) ? returnUrl.trim() : "/billing";
        return UriComponentsBuilder.fromUriString(base)
                .fragment("billing-controls")
                .build(true)
                .toUriString();
    }

    private String generateReference(String prefix, Long tenantId) {
        String seed = Long.toString(Math.abs(System.nanoTime()));
        String suffix = seed.length() > 8 ? seed.substring(seed.length() - 8) : seed;
        return prefix + "-" + tenantId + "-" + suffix;
    }

    private String resolveCardBrand(String reference) {
        String[] brands = {"visa", "mastercard", "verve"};
        int index = Math.abs(reference.hashCode()) % brands.length;
        return brands[index];
    }

    private String resolveCardLast4(String reference) {
        String digits = reference == null ? "" : reference.replaceAll("\\D", "");
        if (digits.length() >= 4) {
            return digits.substring(digits.length() - 4);
        }
        return String.format("%04d", Math.abs(reference.hashCode()) % 10_000);
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String formatDateTime(LocalDateTime value) {
        return value != null ? value.format(RECEIPT_DATE_FORMAT) : "—";
    }
}
