package com.kfarms.service.impl;

import com.kfarms.dto.SupportAssistantChatRequestDto;
import com.kfarms.dto.SupportAssistantHistoryItemDto;
import com.kfarms.dto.SupportTicketCreateRequestDto;
import com.kfarms.dto.SupportTicketMessageRequestDto;
import com.kfarms.dto.SupportTicketStatusUpdateRequestDto;
import com.kfarms.entity.*;
import com.kfarms.repository.*;
import com.kfarms.service.SupportService;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.entity.TenantPlan;
import com.kfarms.tenant.repository.TenantMemberRepository;
import com.kfarms.tenant.repository.TenantRepository;
import com.kfarms.tenant.service.TenantContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SupportServiceImpl implements SupportService {

    private static final String SUPPORT_AUTHOR = "KFarms Support";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM uuuu");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM uuuu HH:mm");

    private static final List<Map<String, Object>> SUPPORT_CHANNELS = List.of(
            channel(
                    "email",
                    "Email Support",
                    "support@kfarms.app",
                    "Replies within one business day.",
                    "mailto:support@kfarms.app"
            ),
            channel(
                    "phone",
                    "Phone Support",
                    "+234 903 5085 579",
                    "Mon - Fri, 9:00 AM to 5:00 PM (WAT).",
                    "tel:+2349035085579"
            ),
            channel(
                    "safety",
                    "Farm Safety Escalation",
                    "Call emergency services first for life-threatening incidents.",
                    "Use app support after immediate safety response.",
                    ""
            )
    );

    private static final List<Map<String, Object>> FARMER_GUIDES = List.of(
            guide(
                    "pond-health-daily",
                    "Fish Ponds",
                    "Daily Pond Health Checklist",
                    "Keep ponds stable with a 10-minute morning and evening routine.",
                    List.of(
                            "Check dissolved oxygen, temperature, and water color before first feed.",
                            "Log unusual fish behavior, gasping, or surface clustering immediately.",
                            "Confirm inlet and outlet flow is normal and record any blockage risk.",
                            "Remove visible debris and verify aeration equipment is running.",
                            "Repeat a quick evening check and note changes in the dashboard."
                    ),
                    "If fish feed response drops suddenly, treat it as an early warning sign."
            ),
            guide(
                    "feed-planning-weekly",
                    "Feeds & Nutrition",
                    "Weekly Feed Planning For Better Margins",
                    "Plan feed purchase and usage by biomass and growth stage.",
                    List.of(
                            "Estimate biomass per pond using latest sample weights and stock counts.",
                            "Apply feeding rate by growth stage, then set daily feed target.",
                            "Cross-check actual feed dispensed against planned quantity.",
                            "Flag ponds with poor appetite and investigate water quality before increasing feed.",
                            "Set reorder thresholds in Inventory to avoid emergency buying."
                    ),
                    "Track feed conversion trends weekly to spot profit leaks early."
            ),
            guide(
                    "livestock-health-logs",
                    "Livestock",
                    "Livestock Health Logging Workflow",
                    "A simple workflow for recording symptoms, treatment, and outcome.",
                    List.of(
                            "Capture affected batch/pen and symptom onset date.",
                            "Record feed change, vaccination status, and recent stress factors.",
                            "Log treatment details and withdrawal period where applicable.",
                            "Set follow-up reminders for review and containment checks.",
                            "Close the case only after condition and mortality trend normalize."
                    ),
                    "Consistent health logs improve both compliance and prevention decisions."
            ),
            guide(
                    "sales-cashflow",
                    "Sales & Revenue",
                    "Sales Recording For Accurate Cashflow",
                    "Capture every sale cleanly so reporting and decisions stay reliable.",
                    List.of(
                            "Record quantity, unit price, buyer details, and payment status same day.",
                            "Tag sales by product line to compare pond and livestock profitability.",
                            "Reconcile cash and transfer entries against your bank or wallet daily.",
                            "Use weekly sales trend view to identify high-demand periods.",
                            "Review overdue invoices at least twice a week."
                    ),
                    "Small data gaps create big finance blind spots over time."
            ),
            guide(
                    "inventory-reorder",
                    "Inventory & Supplies",
                    "Reorder System That Prevents Stockouts",
                    "Build a predictable stock process for feed, treatments, and equipment.",
                    List.of(
                            "Set reorder levels from average weekly usage and supplier lead time.",
                            "Classify items as critical, operational, or low-risk.",
                            "Review low-stock and out-of-stock alerts each morning.",
                            "Track delivery lead times to identify unreliable suppliers.",
                            "Close the loop by confirming received quantities against purchase intent."
                    ),
                    "Critical items should have a safety stock buffer, not just a reorder trigger."
            ),
            guide(
                    "team-workspace-onboarding",
                    "Team & Workspace",
                    "Onboard Team Members Without Data Confusion",
                    "Set roles and responsibilities so records stay clean across teams.",
                    List.of(
                            "Invite users to the correct workspace and confirm access level.",
                            "Define who enters pond data, who reviews, and who approves changes.",
                            "Use a short naming convention for ponds, batches, and stock items.",
                            "Review activity logs weekly for unusual edits or missed records.",
                            "Keep one owner/admin accountable for data quality checks."
                    ),
                    "Clear ownership prevents duplicate records and missing updates."
            )
    );

    private static final List<Map<String, Object>> SUPPORT_FAQS = List.of(
            faq(
                    "faq-1",
                    "How often should I update pond records?",
                    "At least once daily. High-risk periods such as weather changes, disease concern, or feed response drops should be logged more frequently."
            ),
            faq(
                    "faq-2",
                    "Can I track both fish and livestock in one account?",
                    "Yes. KFarms supports fish ponds, livestock, feeds, supplies, sales, and inventory in a single workspace."
            ),
            faq(
                    "faq-3",
                    "What should I do when a critical support issue happens?",
                    "For safety emergencies, contact emergency services first. Then open a high-priority support ticket with clear steps, screenshots, and timestamps."
            ),
            faq(
                    "faq-4",
                    "How do I request plan or billing help?",
                    "Open a support ticket under Billing & Subscription or go to the Billing page to review current plan and payment status."
            )
    );

    private final BillingSubscriptionRepository billingSubscriptionRepository;
    private final FeedRepository feedRepository;
    private final FishPondRepository fishPondRepository;
    private final InventoryRepository inventoryRepository;
    private final LivestockRepository livestockRepository;
    private final NotificationRepository notificationRepository;
    private final SalesRepository salesRepository;
    private final SupportAssistantMessageRepository supportAssistantMessageRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final TaskRepository taskRepository;
    private final TenantMemberRepository tenantMemberRepository;
    private final TenantRepository tenantRepository;

    @Override
    public Map<String, Object> getResources() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("guides", FARMER_GUIDES);
        result.put("faqs", SUPPORT_FAQS);
        result.put("channels", SUPPORT_CHANNELS);
        return result;
    }

    @Override
    public Map<String, Object> getTickets() {
        Long tenantId = requireTenantId();
        List<Map<String, Object>> items = supportTicketRepository.findAllActiveByTenantIdWithMessages(tenantId)
                .stream()
                .map(this::toTicketPayload)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("totalItems", items.size());
        return result;
    }

    @Override
    public Map<String, Object> getPlatformTickets(String search, String status, String lane, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        String normalizedSearch = normalizeText(search).toLowerCase(Locale.ROOT);
        String normalizedStatus = normalizeText(status).toUpperCase(Locale.ROOT);
        String normalizedLane = normalizeText(lane).toUpperCase(Locale.ROOT);

        List<Map<String, Object>> filtered = supportTicketRepository.findAllActiveWithTenantAndMessages()
                .stream()
                .filter((ticket) -> matchesPlatformSearch(ticket, normalizedSearch))
                .filter((ticket) -> matchesPlatformStatus(ticket, normalizedStatus))
                .filter((ticket) -> matchesPlatformLane(ticket, normalizedLane))
                .sorted(Comparator
                        .comparing(SupportTicket::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed()
                        .thenComparing(SupportTicket::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toPlatformTicketPayload)
                .toList();

        int fromIndex = Math.min(safePage * safeSize, filtered.size());
        int toIndex = Math.min(fromIndex + safeSize, filtered.size());
        int totalPages = filtered.isEmpty() ? 1 : (int) Math.ceil((double) filtered.size() / safeSize);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", filtered.subList(fromIndex, toIndex));
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("totalItems", filtered.size());
        result.put("totalPages", totalPages);
        result.put("statusCounts", summarizeTicketsByStatus(filtered));
        result.put("laneCounts", summarizeTicketsByLane(filtered));
        return result;
    }

    @Override
    public Map<String, Object> createTicket(SupportTicketCreateRequestDto request, String actor) {
        Tenant tenant = requireTenant();
        String actorName = resolveActorName(actor);
        LocalDateTime now = LocalDateTime.now();

        SupportTicket ticket = new SupportTicket();
        ticket.setTenant(tenant);
        ticket.setTicketCode(generateTicketCode(tenant.getId()));
        ticket.setSubject(normalizeText(request.getSubject()));
        ticket.setCategory(normalizeText(request.getCategory()));
        ticket.setPriority(parsePriority(request.getPriority()));
        ticket.setStatus(SupportTicketStatus.PENDING);
        ticket.setDescription(normalizeText(request.getDescription()));
        ticket.setCreatedBy(actorName);
        ticket.setUpdatedBy(actorName);
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);

        appendTicketMessage(ticket, tenant, SupportMessageAuthorType.USER, actorName, request.getDescription(), actorName, now);
        appendTicketMessage(ticket, tenant, SupportMessageAuthorType.SUPPORT, SUPPORT_AUTHOR, generateSupportAck(request.getSubject()), SUPPORT_AUTHOR, now.plusSeconds(1));

        SupportTicket saved = supportTicketRepository.save(ticket);
        return ticketEnvelope(saved);
    }

    @Override
    public Map<String, Object> addTicketReply(String ticketId, SupportTicketMessageRequestDto request, String actor) {
        Tenant tenant = requireTenant();
        String actorName = resolveActorName(actor);
        SupportTicket ticket = requireTicket(ticketId, tenant.getId());
        LocalDateTime now = LocalDateTime.now();

        appendTicketMessage(ticket, tenant, SupportMessageAuthorType.USER, actorName, request.getBody(), actorName, now);
        ticket.setStatus(SupportTicketStatus.PENDING);
        ticket.setUpdatedBy(actorName);
        ticket.setUpdatedAt(now);

        SupportTicket saved = supportTicketRepository.save(ticket);
        return ticketEnvelope(saved);
    }

    @Override
    public Map<String, Object> addPlatformTicketReply(String ticketId, SupportTicketMessageRequestDto request, String actor) {
        SupportTicket ticket = requirePlatformTicket(ticketId);
        String actorName = resolveActorName(actor);
        Tenant tenant = ticket.getTenant();
        LocalDateTime now = LocalDateTime.now();

        appendTicketMessage(ticket, tenant, SupportMessageAuthorType.SUPPORT, actorName, request.getBody(), actorName, now);
        ticket.setStatus(SupportTicketStatus.PENDING);
        ticket.setUpdatedBy(actorName);
        ticket.setUpdatedAt(now);

        SupportTicket saved = supportTicketRepository.save(ticket);
        return platformTicketEnvelope(saved);
    }

    @Override
    public Map<String, Object> updateTicketStatus(String ticketId, SupportTicketStatusUpdateRequestDto request, String actor) {
        Tenant tenant = requireTenant();
        String actorName = resolveActorName(actor);
        SupportTicket ticket = requireTicket(ticketId, tenant.getId());
        LocalDateTime now = LocalDateTime.now();

        ticket.setStatus(parseStatus(request.getStatus()));
        ticket.setUpdatedBy(actorName);
        ticket.setUpdatedAt(now);

        SupportTicket saved = supportTicketRepository.save(ticket);
        return ticketEnvelope(saved);
    }

    @Override
    public Map<String, Object> updatePlatformTicketStatus(String ticketId, SupportTicketStatusUpdateRequestDto request, String actor) {
        SupportTicket ticket = requirePlatformTicket(ticketId);
        String actorName = resolveActorName(actor);
        LocalDateTime now = LocalDateTime.now();

        ticket.setStatus(parseStatus(request.getStatus()));
        ticket.setUpdatedBy(actorName);
        ticket.setUpdatedAt(now);

        SupportTicket saved = supportTicketRepository.save(ticket);
        return platformTicketEnvelope(saved);
    }

    @Override
    public Map<String, Object> getAssistantConversation(String actor) {
        Tenant tenant = requireTenant();
        TenantAssistantSnapshot snapshot = buildSnapshot(tenant);
        String actorName = resolveActorName(actor);
        List<SupportAssistantMessage> messages = supportAssistantMessageRepository
                .findByTenant_IdAndDeletedFalseOrderByCreatedAtAscIdAsc(tenant.getId());

        if (messages.isEmpty()) {
            messages = initializeAssistantConversation(tenant, actorName, snapshot);
        }

        return assistantConversationEnvelope(messages, defaultSuggestions(snapshot), defaultActions(snapshot));
    }

    @Override
    public Map<String, Object> resetAssistantConversation(String actor) {
        Tenant tenant = requireTenant();
        TenantAssistantSnapshot snapshot = buildSnapshot(tenant);
        String actorName = resolveActorName(actor);
        supportAssistantMessageRepository.deleteAllByTenantId(tenant.getId());
        List<SupportAssistantMessage> messages = initializeAssistantConversation(tenant, actorName, snapshot);
        return assistantConversationEnvelope(messages, defaultSuggestions(snapshot), defaultActions(snapshot));
    }

    @Override
    public Map<String, Object> chat(SupportAssistantChatRequestDto request, String actor) {
        Tenant tenant = requireTenant();
        TenantAssistantSnapshot snapshot = buildSnapshot(tenant);
        String actorName = resolveUserName(request, actor);
        String content = normalizeText(request.getMessage());

        List<SupportAssistantMessage> existing = supportAssistantMessageRepository
                .findByTenant_IdAndDeletedFalseOrderByCreatedAtAscIdAsc(tenant.getId());
        if (existing.isEmpty()) {
            initializeAssistantConversation(tenant, actorName, snapshot);
        }

        SupportAssistantMessage userMessage = new SupportAssistantMessage();
        userMessage.setTenant(tenant);
        userMessage.setRole(SupportAssistantRole.USER);
        userMessage.setContent(content);
        userMessage.setCreatedBy(actorName);
        userMessage.setUpdatedBy(actorName);
        supportAssistantMessageRepository.save(userMessage);

        AssistantIntent intent = resolveIntent(request);
        AssistantReply assistantReply = generateAssistantReply(intent, content, snapshot);

        SupportAssistantMessage assistantMessage = new SupportAssistantMessage();
        assistantMessage.setTenant(tenant);
        assistantMessage.setRole(SupportAssistantRole.ASSISTANT);
        assistantMessage.setContent(assistantReply.reply());
        assistantMessage.setCreatedBy(SUPPORT_AUTHOR);
        assistantMessage.setUpdatedBy(SUPPORT_AUTHOR);
        supportAssistantMessageRepository.save(assistantMessage);

        List<SupportAssistantMessage> messages = supportAssistantMessageRepository
                .findByTenant_IdAndDeletedFalseOrderByCreatedAtAscIdAsc(tenant.getId());

        Map<String, Object> result = assistantConversationEnvelope(messages, assistantReply.suggestions(), assistantReply.actions());
        result.put("reply", assistantReply.reply());
        result.put("intent", intent.name().toLowerCase(Locale.ROOT));
        return result;
    }

    private TenantAssistantSnapshot buildSnapshot(Tenant tenant) {
        Long tenantId = tenant.getId();
        LocalDate today = LocalDate.now();
        return new TenantAssistantSnapshot(
                tenant,
                inventoryRepository.findAllActiveByTenantId(tenantId),
                feedRepository.findRecentActiveByTenantId(tenantId, PageRequest.of(0, 5)),
                feedRepository.findActiveByTenantIdAndDateBetween(tenantId, today.minusDays(6), today),
                salesRepository.findRecentActiveByTenantId(tenantId, PageRequest.of(0, 5)),
                salesRepository.findActiveByTenantIdAndSalesDateBetween(tenantId, today.minusDays(6), today),
                fishPondRepository.findAllActiveByTenantId(tenantId),
                livestockRepository.findAllActive(tenantId),
                taskRepository.findActiveByTenantIdAndStatus(tenantId, TaskStatus.PENDING),
                notificationRepository.findUnreadByTenantId(tenantId, PageRequest.of(0, 5)),
                supportTicketRepository.findAllActiveByTenantIdWithMessages(tenantId),
                tenantMemberRepository.countByTenant_Id(tenantId),
                billingSubscriptionRepository.findByTenant_Id(tenantId).orElse(null)
        );
    }

    private Map<String, Object> ticketEnvelope(SupportTicket ticket) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ticket", toTicketPayload(ticket));
        return result;
    }

    private Map<String, Object> assistantConversationEnvelope(
            List<SupportAssistantMessage> messages,
            List<String> suggestions,
            List<Map<String, Object>> actions
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("messages", messages.stream().map(this::toAssistantMessagePayload).toList());
        result.put("suggestions", uniqueSuggestions(suggestions));
        result.put("actions", limitActions(actions));
        return result;
    }

    private Map<String, Object> platformTicketEnvelope(SupportTicket ticket) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ticket", toPlatformTicketPayload(ticket));
        return result;
    }

    private SupportTicket requireTicket(String ticketId, Long tenantId) {
        String normalizedTicketId = normalizeText(ticketId);
        if (normalizedTicketId.isBlank()) {
            throw new IllegalArgumentException("Ticket id is required.");
        }

        return supportTicketRepository.findByTicketCodeAndTenantIdWithMessages(normalizedTicketId, tenantId)
                .or(() -> parseLong(normalizedTicketId)
                        .flatMap(id -> supportTicketRepository.findByIdAndTenantIdWithMessages(id, tenantId)))
                .orElseThrow(() -> new IllegalArgumentException("Support ticket not found."));
    }

    private SupportTicket requirePlatformTicket(String ticketId) {
        String normalizedTicketId = normalizeText(ticketId);
        if (normalizedTicketId.isBlank()) {
            throw new IllegalArgumentException("Ticket id is required.");
        }

        return supportTicketRepository.findAllActiveWithTenantAndMessages()
                .stream()
                .filter((ticket) -> normalizedTicketId.equalsIgnoreCase(ticket.getTicketCode())
                        || parseLong(normalizedTicketId).map((id) -> Objects.equals(ticket.getId(), id)).orElse(false))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Support ticket not found."));
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Missing X-Tenant-Id header.");
        }
        return tenantId;
    }

    private Tenant requireTenant() {
        Long tenantId = requireTenantId();
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found."));
    }

    private List<SupportAssistantMessage> initializeAssistantConversation(
            Tenant tenant,
            String actorName,
            TenantAssistantSnapshot snapshot
    ) {
        SupportAssistantMessage welcomeMessage = new SupportAssistantMessage();
        welcomeMessage.setTenant(tenant);
        welcomeMessage.setRole(SupportAssistantRole.ASSISTANT);
        welcomeMessage.setContent(buildWelcomeMessage(actorName, snapshot));
        welcomeMessage.setCreatedBy(SUPPORT_AUTHOR);
        welcomeMessage.setUpdatedBy(SUPPORT_AUTHOR);
        supportAssistantMessageRepository.save(welcomeMessage);
        return supportAssistantMessageRepository.findByTenant_IdAndDeletedFalseOrderByCreatedAtAscIdAsc(tenant.getId());
    }

    private void appendTicketMessage(
            SupportTicket ticket,
            Tenant tenant,
            SupportMessageAuthorType authorType,
            String authorName,
            String body,
            String actor,
            LocalDateTime createdAt
    ) {
        SupportTicketMessage message = new SupportTicketMessage();
        message.setTicket(ticket);
        message.setTenant(tenant);
        message.setAuthorType(authorType);
        message.setAuthorName(normalizeText(authorName));
        message.setBody(normalizeText(body));
        message.setCreatedBy(actor);
        message.setUpdatedBy(actor);
        message.setCreatedAt(createdAt);
        message.setUpdatedAt(createdAt);
        ticket.getMessages().add(message);
    }

    private AssistantIntent resolveIntent(SupportAssistantChatRequestDto request) {
        String message = request != null ? normalizeText(request.getMessage()).toLowerCase(Locale.ROOT) : "";
        AssistantIntent directIntent = detectIntent(message);
        if (directIntent != AssistantIntent.UNKNOWN) {
            return directIntent;
        }

        if (request != null && request.getHistory() != null) {
            List<SupportAssistantHistoryItemDto> history = request.getHistory();
            for (int index = history.size() - 1; index >= 0; index--) {
                SupportAssistantHistoryItemDto entry = history.get(index);
                if (entry == null || !"user".equalsIgnoreCase(normalizeText(entry.getRole()))) {
                    continue;
                }
                String candidate = normalizeText(entry.getContent()).toLowerCase(Locale.ROOT);
                if (candidate.isBlank() || candidate.equals(message)) {
                    continue;
                }
                AssistantIntent intent = detectIntent(candidate);
                if (intent != AssistantIntent.UNKNOWN && intent != AssistantIntent.GREETING) {
                    return intent;
                }
            }
        }

        return AssistantIntent.OVERVIEW;
    }

    private AssistantIntent detectIntent(String text) {
        if (text.isBlank()) {
            return AssistantIntent.OVERVIEW;
        }
        if (isGreeting(text)) {
            return AssistantIntent.GREETING;
        }
        if (containsAny(text, "today", "attention", "summary", "overview", "snapshot", "status", "urgent", "what can you see")) {
            return AssistantIntent.OVERVIEW;
        }
        if (containsAny(text, "task", "todo", "reminder", "overdue", "due")) {
            return AssistantIntent.TASKS;
        }
        if (containsAny(text, "inventory", "stock", "reorder", "supplier", "suppl")) {
            return AssistantIntent.INVENTORY;
        }
        if (containsAny(text, "feed", "fcr", "nutrition", "biomass")) {
            return AssistantIntent.FEEDS;
        }
        if (containsAny(text, "mortality", "disease", "sick", "oxygen", "water", "aeration", "pond", "fish")) {
            return AssistantIntent.PONDS;
        }
        if (containsAny(text, "livestock", "batch", "broiler", "layer", "noiler", "goat", "poultry", "bird")) {
            return AssistantIntent.LIVESTOCK;
        }
        if (containsAny(text, "sales", "cash", "invoice", "revenue", "buyer")) {
            return AssistantIntent.SALES;
        }
        if (containsAny(text, "billing", "payment", "subscription", "plan", "pro", "enterprise")) {
            return AssistantIntent.BILLING;
        }
        if (containsAny(text, "team", "invite", "member", "workspace", "tenant", "role", "staff", "user")) {
            return AssistantIntent.TEAM;
        }
        if (containsAny(text, "support", "help", "ticket", "faq", "guide")) {
            return AssistantIntent.SUPPORT;
        }
        return AssistantIntent.UNKNOWN;
    }

    private AssistantReply generateAssistantReply(
            AssistantIntent intent,
            String message,
            TenantAssistantSnapshot snapshot
    ) {
        String text = normalizeText(message).toLowerCase(Locale.ROOT);

        if (containsAny(text, "daily pond checklist", "pond checklist")) {
            return buildGuideReply("pond-health-daily", snapshot);
        }
        if (containsAny(text, "feed planning", "avoid feed stockout", "feed stockout")) {
            return buildFeedReply(snapshot, text);
        }
        if (containsAny(text, "open a support ticket", "create a support ticket", "report an issue")) {
            return buildSupportReply(snapshot, text, true);
        }

        return switch (intent) {
            case GREETING, OVERVIEW, UNKNOWN -> buildOverviewReply(snapshot);
            case INVENTORY -> buildInventoryReply(snapshot, text);
            case FEEDS -> buildFeedReply(snapshot, text);
            case PONDS -> buildPondReply(snapshot, text);
            case LIVESTOCK -> buildLivestockReply(snapshot);
            case SALES -> buildSalesReply(snapshot);
            case TASKS -> buildTaskReply(snapshot);
            case SUPPORT -> buildSupportReply(snapshot, text, false);
            case BILLING -> buildBillingReply(snapshot, text);
            case TEAM -> buildTeamReply(snapshot);
        };
    }

    private AssistantReply buildOverviewReply(TenantAssistantSnapshot snapshot) {
        List<String> lines = new ArrayList<>();
        lines.add("Here is the current workspace snapshot for " + snapshot.tenant().getName() + ":");

        if (!workspaceHasOperationalData(snapshot)) {
            lines.add("- I do not see much farm activity recorded yet.");
            lines.add("- Start with inventory, ponds, feeds, or sales so I can give you more specific guidance.");
            return new AssistantReply(
                    String.join("\n", lines),
                    List.of(
                            "How do I set up fish ponds?",
                            "How do I avoid feed stockout?",
                            "How do I open a support ticket?"
                    ),
                    List.of(
                            navigateAction("open-fish-ponds", "Open Fish Ponds", "/fish-ponds"),
                            navigateAction("open-inventory", "Open Inventory", "/inventory"),
                            navigateAction("open-support", "Open Support", "/support")
                    )
            );
        }

        lines.add("- Inventory: " + lowStockItems(snapshot).size() + " low stock, " + outOfStockItems(snapshot).size() + " out of stock.");
        lines.add("- Tasks: " + overdueTasks(snapshot).size() + " overdue, " + dueSoonTasks(snapshot).size() + " due soon.");
        if (!snapshot.ponds().isEmpty()) {
            lines.add("- Fish ponds: " + activePonds(snapshot) + " active, total stock " + formatNumber(totalFishStock(snapshot)) + ".");
        }
        if (!snapshot.livestockGroups().isEmpty()) {
            lines.add("- Livestock: " + snapshot.livestockGroups().size() + " groups, total stock " + formatNumber(totalLivestockStock(snapshot)) + ".");
        }
        if (!snapshot.weeklySales().isEmpty()) {
            lines.add("- Sales: " + formatCurrency(sumSales(snapshot.weeklySales())) + " in the last 7 days.");
        }
        if (unresolvedTicketCount(snapshot) > 0) {
            lines.add("- Support: " + unresolvedTicketCount(snapshot) + " unresolved ticket(s).");
        }

        List<String> attention = topAttentionAreas(snapshot);
        if (!attention.isEmpty()) {
            lines.add("Priority attention:");
            attention.forEach(item -> lines.add("- " + item));
        }

        return new AssistantReply(
                String.join("\n", lines),
                defaultSuggestions(snapshot),
                defaultActions(snapshot)
        );
    }

    private AssistantReply buildInventoryReply(TenantAssistantSnapshot snapshot, String text) {
        List<Inventory> urgent = containsAny(text, "feed stockout", "stockout")
                ? urgentFeedInventoryItems(snapshot)
                : urgentInventoryItems(snapshot);

        List<String> lines = new ArrayList<>();
        if (snapshot.inventoryItems().isEmpty()) {
            lines.add("I do not see inventory records in this workspace yet.");
            lines.add("Create feed and supply items first so I can warn you about low stock and reorder risk.");
            return new AssistantReply(
                    String.join("\n", lines),
                    List.of("How do I avoid feed stockout?", "What needs attention today?", "How do I open a support ticket?"),
                    List.of(
                            navigateAction("open-inventory", "Open Inventory", "/inventory"),
                            navigateAction("open-support", "Open Support", "/support")
                    )
            );
        }

        lines.add("Inventory snapshot for " + snapshot.tenant().getName() + ":");
        lines.add("- " + snapshot.inventoryItems().size() + " active item(s) tracked.");
        lines.add("- " + lowStockItems(snapshot).size() + " item(s) are low stock and " + outOfStockItems(snapshot).size() + " item(s) are out of stock.");
        lines.add("- Estimated inventory value: " + formatCurrency(totalInventoryValue(snapshot)) + ".");
        if (!urgent.isEmpty()) {
            lines.add("Most urgent items:");
            urgent.stream().limit(3).forEach(item -> lines.add("- " + formatInventoryItem(item)));
        }
        lines.add("Recommendation: replenish the out-of-stock items first, then review reorder thresholds for the low-stock items.");

        return new AssistantReply(
                String.join("\n", lines),
                List.of(
                        "Which feed items are low?",
                        "What needs attention today?",
                        "How do I open a support ticket?"
                ),
                List.of(
                        navigateAction("open-inventory", "Open Inventory", "/inventory"),
                        navigateAction("open-support-ticket", "Open Inventory Ticket", buildSupportTicketTarget(
                                "Inventory & Supplies",
                                outOfStockItems(snapshot).isEmpty() ? "MEDIUM" : "HIGH",
                                "Need help with inventory alerts",
                                "Please help review urgent inventory alerts in my workspace."
                        ))
                )
        );
    }

    private AssistantReply buildFeedReply(TenantAssistantSnapshot snapshot, String text) {
        List<Inventory> feedRisk = urgentFeedInventoryItems(snapshot);
        List<String> lines = new ArrayList<>();

        if (snapshot.weeklyFeeds().isEmpty()) {
            lines.add("I do not see feed usage records from the last 7 days yet.");
            if (!feedRisk.isEmpty()) {
                lines.add("I can still see feed-related stock risk:");
                feedRisk.stream().limit(3).forEach(item -> lines.add("- " + formatInventoryItem(item)));
            }
            lines.add("Start logging feed usage so I can compare weekly usage against stock levels.");
            return new AssistantReply(
                    String.join("\n", lines),
                    List.of("How do I avoid feed stockout?", "What needs attention today?", "Show me a daily pond checklist"),
                    List.of(
                            navigateAction("open-feeds", "Open Feeds", "/feeds"),
                            navigateAction("open-inventory", "Open Inventory", "/inventory")
                    )
            );
        }

        lines.add("Feed snapshot for " + snapshot.tenant().getName() + ":");
        lines.add("- " + formatNumber(totalFeedUsed(snapshot.weeklyFeeds())) + " unit(s) logged in the last 7 days.");
        if (!snapshot.recentFeeds().isEmpty()) {
            lines.add("Recent feed entries:");
            snapshot.recentFeeds().stream().limit(3).forEach(feed -> lines.add("- " + formatFeedEntry(feed)));
        }
        if (!feedRisk.isEmpty()) {
            lines.add("Feed-related stock risk:");
            feedRisk.stream().limit(3).forEach(item -> lines.add("- " + formatInventoryItem(item)));
        } else {
            lines.add("No urgent feed stock risk is visible right now.");
        }
        lines.add("Recommendation: review actual feed dispensed against plan daily and keep safety stock for the most critical feed lines.");

        return new AssistantReply(
                String.join("\n", lines),
                List.of(
                        "Which inventory items need reorder?",
                        "Show me a daily pond checklist",
                        "What needs attention today?"
                ),
                List.of(
                        navigateAction("open-feeds", "Open Feeds", "/feeds"),
                        navigateAction("open-inventory", "Open Inventory", "/inventory")
                )
        );
    }

    private AssistantReply buildPondReply(TenantAssistantSnapshot snapshot, String text) {
        List<String> lines = new ArrayList<>();
        if (snapshot.ponds().isEmpty()) {
            lines.add("I do not see fish pond records in this workspace yet.");
            lines.add("Set up your ponds first so I can track stock, mortality, maintenance, and water-change attention.");
            return new AssistantReply(
                    String.join("\n", lines),
                    List.of("Show me a daily pond checklist", "What needs attention today?", "How do I open a support ticket?"),
                    List.of(
                            navigateAction("open-fish-ponds", "Open Fish Ponds", "/fish-ponds"),
                            navigateAction("open-support", "Open Support", "/support")
                    )
            );
        }

        lines.add("Fish pond snapshot for " + snapshot.tenant().getName() + ":");
        lines.add("- " + activePonds(snapshot) + " active, " + maintenancePonds(snapshot) + " in maintenance, " + emptyPonds(snapshot) + " empty.");
        lines.add("- Total fish stock: " + formatNumber(totalFishStock(snapshot)) + ".");
        lines.add("- Recorded mortality: " + formatNumber(totalPondMortality(snapshot)) + ".");

        List<FishPond> mortalityRisk = topMortalityPonds(snapshot);
        if (!mortalityRisk.isEmpty()) {
            lines.add("Highest mortality ponds:");
            mortalityRisk.stream().limit(3).forEach(pond -> lines.add("- " + formatPondMortality(pond)));
        }

        List<FishPond> waterAttention = staleWaterChangePonds(snapshot);
        if (!waterAttention.isEmpty()) {
            lines.add("Water-change attention:");
            waterAttention.stream().limit(2).forEach(pond -> lines.add("- " + formatWaterAttention(pond)));
        }

        if (containsAny(text, "mortality", "disease", "sick", "oxygen", "water", "aeration")) {
            lines.add("Immediate checks: verify dissolved oxygen and temperature, inspect fish behavior, and reduce feeding if fish are stressed.");
        } else {
            lines.add("Recommendation: keep daily pond checks tight and treat sudden feed-response drops as early warning signals.");
        }

        return new AssistantReply(
                String.join("\n", lines),
                List.of(
                        "Show me a daily pond checklist",
                        "What needs attention today?",
                        "How do I open a support ticket?"
                ),
                List.of(
                        navigateAction("open-fish-ponds", "Open Fish Ponds", "/fish-ponds"),
                        navigateAction("open-health-ticket", "Open Pond Support Ticket", buildSupportTicketTarget(
                                "Fish Ponds",
                                totalPondMortality(snapshot) > 0 || containsAny(text, "mortality", "disease", "sick") ? "HIGH" : "MEDIUM",
                                "Need help with pond health",
                                "Please help review a pond health issue in my workspace."
                        ))
                )
        );
    }

    private AssistantReply buildLivestockReply(TenantAssistantSnapshot snapshot) {
        List<String> lines = new ArrayList<>();
        if (snapshot.livestockGroups().isEmpty()) {
            lines.add("I do not see livestock groups in this workspace yet.");
            lines.add("Record your livestock batches first so I can help with stock, mortality, and follow-up advice.");
            return new AssistantReply(
                    String.join("\n", lines),
                    List.of("What needs attention today?", "How do I open a support ticket?", "How do I onboard new staff?"),
                    List.of(
                            navigateAction("open-livestock", "Open Livestock", "/livestock"),
                            navigateAction("open-support", "Open Support", "/support")
                    )
            );
        }

        lines.add("Livestock snapshot for " + snapshot.tenant().getName() + ":");
        lines.add("- " + snapshot.livestockGroups().size() + " active group(s) tracked.");
        lines.add("- Total livestock stock: " + formatNumber(totalLivestockStock(snapshot)) + ".");
        lines.add("- Recorded mortality: " + formatNumber(totalLivestockMortality(snapshot)) + ".");
        Livestock largestGroup = largestLivestockGroup(snapshot);
        if (largestGroup != null) {
            lines.add("- Largest group: " + normalizeText(largestGroup.getBatchName()) + " with " + formatNumber(Optional.ofNullable(largestGroup.getCurrentStock()).orElse(0)) + " animals.");
        }
        lines.add("Recommendation: keep symptom, feed-change, and treatment logs together so issues are easier to review.");

        return new AssistantReply(
                String.join("\n", lines),
                List.of(
                        "What needs attention today?",
                        "How do I open a support ticket?",
                        "How do I onboard new staff?"
                ),
                List.of(
                        navigateAction("open-livestock", "Open Livestock", "/livestock"),
                        navigateAction("open-support", "Open Support", "/support")
                )
        );
    }

    private AssistantReply buildSalesReply(TenantAssistantSnapshot snapshot) {
        List<String> lines = new ArrayList<>();
        if (snapshot.recentSales().isEmpty()) {
            lines.add("I do not see sales records in this workspace yet.");
            lines.add("Log sales with quantity, buyer, unit price, and payment status so I can help with cashflow insight.");
            return new AssistantReply(
                    String.join("\n", lines),
                    List.of("What needs attention today?", "How can I improve reporting accuracy?", "How do I open a support ticket?"),
                    List.of(
                            navigateAction("open-sales", "Open Sales", "/sales"),
                            navigateAction("open-support", "Open Support", "/support")
                    )
            );
        }

        lines.add("Sales snapshot for " + snapshot.tenant().getName() + ":");
        lines.add("- Today: " + formatCurrency(salesForDate(snapshot.weeklySales(), LocalDate.now())) + ".");
        lines.add("- Last 7 days: " + formatCurrency(sumSales(snapshot.weeklySales())) + ".");
        Sales latestSale = snapshot.recentSales().get(0);
        lines.add("- Latest sale: " + formatSale(latestSale) + ".");
        lines.add("Recommendation: keep recording sales same day and review unpaid or delayed buyer follow-up twice weekly.");

        return new AssistantReply(
                String.join("\n", lines),
                List.of(
                        "What needs attention today?",
                        "What sales fields are mandatory?",
                        "How can I improve reporting accuracy?"
                ),
                List.of(
                        navigateAction("open-sales", "Open Sales", "/sales"),
                        navigateAction("open-billing", "Open Billing", "/billing")
                )
        );
    }

    private AssistantReply buildTaskReply(TenantAssistantSnapshot snapshot) {
        List<Task> overdue = overdueTasks(snapshot);
        List<Task> dueSoon = dueSoonTasks(snapshot);
        List<String> lines = new ArrayList<>();

        if (snapshot.pendingTasks().isEmpty()) {
            lines.add("There are no pending tasks in this workspace right now.");
            lines.add("If you want tighter operations follow-up, add recurring or one-off tasks from the dashboard.");
            return new AssistantReply(
                    String.join("\n", lines),
                    List.of("What needs attention today?", "How do I avoid feed stockout?", "Show me a daily pond checklist"),
                    List.of(
                            navigateAction("open-dashboard", "Open Dashboard", "/dashboard"),
                            navigateAction("open-support", "Open Support", "/support")
                    )
            );
        }

        lines.add("Task snapshot for " + snapshot.tenant().getName() + ":");
        lines.add("- " + overdue.size() + " overdue and " + dueSoon.size() + " due soon.");
        lines.add("Closest pending tasks:");
        snapshot.pendingTasks().stream().limit(3).forEach(task -> lines.add("- " + formatTask(task)));
        lines.add("Recommendation: clear overdue items first, then review the tasks due within the next 48 hours.");

        return new AssistantReply(
                String.join("\n", lines),
                List.of(
                        "What needs attention today?",
                        "Which inventory items need reorder?",
                        "How do I open a support ticket?"
                ),
                List.of(
                        navigateAction("open-dashboard", "Open Dashboard", "/dashboard"),
                        navigateAction("open-support", "Open Support", "/support")
                )
        );
    }

    private AssistantReply buildSupportReply(
            TenantAssistantSnapshot snapshot,
            String text,
            boolean explicitTicketRequest
    ) {
        long open = snapshot.tickets().stream().filter(ticket -> ticket.getStatus() == SupportTicketStatus.OPEN).count();
        long pending = snapshot.tickets().stream().filter(ticket -> ticket.getStatus() == SupportTicketStatus.PENDING).count();
        long resolved = snapshot.tickets().stream().filter(ticket -> ticket.getStatus() == SupportTicketStatus.RESOLVED).count();

        List<String> lines = new ArrayList<>();
        lines.add("Support snapshot for " + snapshot.tenant().getName() + ":");
        lines.add("- " + snapshot.tickets().size() + " total ticket(s): " + open + " open, " + pending + " pending, " + resolved + " resolved.");
        if (!snapshot.tickets().isEmpty()) {
            SupportTicket latest = snapshot.tickets().get(0);
            lines.add("- Latest ticket: " + latest.getSubject() + " (" + latest.getStatus().name() + ").");
        }

        if (explicitTicketRequest || containsAny(text, "open", "create", "ticket", "report")) {
            lines.add("For the fastest resolution, include what happened, when it happened, the affected module, and what you already tried.");
        } else {
            lines.add("You can use the Support Center to browse guides, review FAQs, or open a tracked ticket.");
        }

        String category = resolveSupportCategory(text, AssistantIntent.SUPPORT);
        String priority = resolveSupportPriority(text);
        return new AssistantReply(
                String.join("\n", lines),
                List.of(
                        "How do I open a support ticket?",
                        "Show farmer guides",
                        "What needs attention today?"
                ),
                List.of(
                        navigateAction("open-support", "Open Support Center", "/support"),
                        navigateAction("open-ticket", "Open Support Ticket", buildSupportTicketTarget(
                                category,
                                priority,
                                "Need help from KFarms support",
                                "Please help me review an issue in my workspace."
                        ))
                )
        );
    }

    private AssistantReply buildBillingReply(TenantAssistantSnapshot snapshot, String text) {
        BillingSubscription subscription = snapshot.billingSubscription();
        TenantPlan plan = subscription != null && subscription.getPlan() != null
                ? subscription.getPlan()
                : snapshot.tenant().getPlan();
        BillingSubscriptionStatus status = subscription != null && subscription.getStatus() != null
                ? subscription.getStatus()
                : BillingSubscriptionStatus.ACTIVE;

        List<String> lines = new ArrayList<>();
        lines.add("Billing snapshot for " + snapshot.tenant().getName() + ":");
        lines.add("- Current plan: " + plan.name() + ".");
        lines.add("- Subscription status: " + status.name() + ".");
        if (subscription != null && subscription.getNextBillingDate() != null) {
            lines.add("- Next billing date: " + formatDate(subscription.getNextBillingDate()) + ".");
        }
        if (subscription != null && Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd())) {
            lines.add("- Cancellation at period end is already scheduled.");
        }
        lines.add("Recommendation: review invoices and payment status in Billing, then open a ticket if any payment reference looks wrong.");

        return new AssistantReply(
                String.join("\n", lines),
                List.of(
                        "How do I report a failed payment?",
                        "What needs attention today?",
                        "How do I open a support ticket?"
                ),
                List.of(
                        navigateAction("open-billing", "Open Billing", "/billing"),
                        navigateAction("open-billing-ticket", "Open Billing Ticket", buildSupportTicketTarget(
                                "Billing & Subscription",
                                resolveSupportPriority(text),
                                "Need help with billing",
                                "Please help review a billing or subscription issue in my workspace."
                        ))
                )
        );
    }

    private AssistantReply buildTeamReply(TenantAssistantSnapshot snapshot) {
        List<String> lines = new ArrayList<>();
        lines.add("Team snapshot for " + snapshot.tenant().getName() + ":");
        lines.add("- " + snapshot.memberCount() + " active member(s) in this workspace.");
        lines.add("- Keep roles clear for data entry, review, and approval so records stay consistent.");
        lines.add("Recommendation: review naming conventions for ponds, batches, and stock items with the team each week.");

        return new AssistantReply(
                String.join("\n", lines),
                List.of(
                        "How do I onboard new staff?",
                        "What role setup is best for small teams?",
                        "What needs attention today?"
                ),
                List.of(
                        navigateAction("open-settings", "Open Settings", "/settings"),
                        navigateAction("open-support", "Open Support", "/support")
                )
        );
    }

    private AssistantReply buildGuideReply(String guideId, TenantAssistantSnapshot snapshot) {
        Map<String, Object> guide = findGuideById(guideId).orElse(null);
        if (guide == null) {
            return buildOverviewReply(snapshot);
        }

        @SuppressWarnings("unchecked")
        List<String> steps = (List<String>) guide.getOrDefault("steps", List.of());
        List<String> lines = new ArrayList<>();
        lines.add(String.valueOf(guide.getOrDefault("title", "Farmer Guide")));
        lines.add(String.valueOf(guide.getOrDefault("summary", "")));
        for (int index = 0; index < steps.size(); index++) {
            lines.add((index + 1) + ". " + steps.get(index));
        }
        Object tip = guide.get("tip");
        if (tip instanceof String tipText && !tipText.isBlank()) {
            lines.add("Tip: " + tipText.trim());
        }

        return new AssistantReply(
                String.join("\n", lines),
                List.of(
                        "What needs attention today?",
                        "How do I avoid feed stockout?",
                        "How do I open a support ticket?"
                ),
                List.of(
                        navigateAction("open-fish-ponds", "Open Fish Ponds", "/fish-ponds"),
                        navigateAction("open-support", "Open Support", "/support")
                )
        );
    }

    private String buildWelcomeMessage(String actorName, TenantAssistantSnapshot snapshot) {
        String safeActor = normalizeText(actorName).isBlank() ? "Farmer" : normalizeText(actorName);
        List<String> lines = new ArrayList<>();
        lines.add("Hi " + safeActor + ". I am your KFarms Assistant for " + snapshot.tenant().getName() + ".");

        if (!workspaceHasOperationalData(snapshot)) {
            lines.add("I do not see much farm activity recorded yet, so start by adding ponds, inventory, feeds, or sales.");
            lines.add("Ask me about setup steps, support tickets, or how to avoid feed stockout.");
            return String.join("\n", lines);
        }

        lines.add("Current workspace snapshot:");
        lines.add("- Inventory: " + lowStockItems(snapshot).size() + " low stock, " + outOfStockItems(snapshot).size() + " out of stock.");
        lines.add("- Tasks: " + overdueTasks(snapshot).size() + " overdue, " + dueSoonTasks(snapshot).size() + " due soon.");
        if (!snapshot.ponds().isEmpty()) {
            lines.add("- Fish ponds: " + activePonds(snapshot) + " active with " + formatNumber(totalFishStock(snapshot)) + " fish in stock.");
        }
        if (!snapshot.weeklySales().isEmpty()) {
            lines.add("- Sales: " + formatCurrency(sumSales(snapshot.weeklySales())) + " in the last 7 days.");
        }
        lines.add("Ask me about inventory, feeds, ponds, livestock, sales, billing, or support.");
        return String.join("\n", lines);
    }

    private List<String> defaultSuggestions(TenantAssistantSnapshot snapshot) {
        List<String> suggestions = new ArrayList<>();
        if (!lowStockItems(snapshot).isEmpty()) {
            suggestions.add("Which inventory items need reorder?");
        }
        if (!dueSoonTasks(snapshot).isEmpty() || !overdueTasks(snapshot).isEmpty()) {
            suggestions.add("What needs attention today?");
        }
        if (!snapshot.ponds().isEmpty()) {
            suggestions.add("Show me a daily pond checklist");
        }
        if (!snapshot.weeklySales().isEmpty()) {
            suggestions.add("How are sales this week?");
        }
        if (suggestions.size() < 3) {
            suggestions.add("How do I avoid feed stockout?");
            suggestions.add("How do I open a support ticket?");
            suggestions.add("What can you help me with?");
        }
        return uniqueSuggestions(suggestions);
    }

    private List<Map<String, Object>> defaultActions(TenantAssistantSnapshot snapshot) {
        List<Map<String, Object>> actions = new ArrayList<>();
        if (!lowStockItems(snapshot).isEmpty()) {
            actions.add(navigateAction("open-inventory", "Open Inventory", "/inventory"));
        }
        if (!dueSoonTasks(snapshot).isEmpty() || !overdueTasks(snapshot).isEmpty()) {
            actions.add(navigateAction("open-dashboard", "Open Dashboard", "/dashboard"));
        }
        if (!snapshot.ponds().isEmpty()) {
            actions.add(navigateAction("open-fish-ponds", "Open Fish Ponds", "/fish-ponds"));
        }
        if (actions.size() < 3) {
            actions.add(navigateAction("open-support", "Open Support", "/support"));
        }
        return limitActions(actions);
    }

    private List<String> topAttentionAreas(TenantAssistantSnapshot snapshot) {
        List<String> issues = new ArrayList<>();
        if (!outOfStockItems(snapshot).isEmpty()) {
            issues.add(outOfStockItems(snapshot).size() + " inventory item(s) are out of stock.");
        }
        if (!overdueTasks(snapshot).isEmpty()) {
            issues.add(overdueTasks(snapshot).size() + " task(s) are overdue.");
        }
        if (maintenancePonds(snapshot) > 0) {
            issues.add(maintenancePonds(snapshot) + " pond(s) are in maintenance.");
        }
        if (totalPondMortality(snapshot) > 0) {
            issues.add("Recorded pond mortality is " + formatNumber(totalPondMortality(snapshot)) + ".");
        }
        if (unresolvedTicketCount(snapshot) > 0) {
            issues.add(unresolvedTicketCount(snapshot) + " support ticket(s) are unresolved.");
        }
        if (!snapshot.unreadNotifications().isEmpty()) {
            issues.add("Unread alerts include " + quote(normalizeText(snapshot.unreadNotifications().get(0).getTitle())) + ".");
        }
        return issues.stream().limit(3).toList();
    }

    private boolean workspaceHasOperationalData(TenantAssistantSnapshot snapshot) {
        return !snapshot.inventoryItems().isEmpty()
                || !snapshot.weeklyFeeds().isEmpty()
                || !snapshot.recentSales().isEmpty()
                || !snapshot.ponds().isEmpty()
                || !snapshot.livestockGroups().isEmpty()
                || !snapshot.pendingTasks().isEmpty()
                || !snapshot.tickets().isEmpty();
    }

    private List<Inventory> lowStockItems(TenantAssistantSnapshot snapshot) {
        return snapshot.inventoryItems().stream()
                .filter(item -> quantity(item) > 0)
                .filter(item -> quantity(item) <= minThreshold(item))
                .sorted(Comparator.comparingInt(this::quantity))
                .toList();
    }

    private List<Inventory> outOfStockItems(TenantAssistantSnapshot snapshot) {
        return snapshot.inventoryItems().stream()
                .filter(item -> quantity(item) <= 0)
                .sorted(Comparator.comparing(Inventory::getItemName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<Inventory> urgentInventoryItems(TenantAssistantSnapshot snapshot) {
        List<Inventory> urgent = new ArrayList<>();
        urgent.addAll(outOfStockItems(snapshot));
        urgent.addAll(lowStockItems(snapshot));
        return urgent.stream()
                .distinct()
                .limit(5)
                .toList();
    }

    private List<Inventory> urgentFeedInventoryItems(TenantAssistantSnapshot snapshot) {
        return urgentInventoryItems(snapshot).stream()
                .filter(this::isFeedRelatedInventory)
                .toList();
    }

    private boolean isFeedRelatedInventory(Inventory item) {
        InventoryCategory category = item.getCategory();
        return category == InventoryCategory.FEED
                || category == InventoryCategory.FISH
                || category == InventoryCategory.LAYER
                || category == InventoryCategory.NOILER;
    }

    private BigDecimal totalInventoryValue(TenantAssistantSnapshot snapshot) {
        return snapshot.inventoryItems().stream()
                .map(item -> Optional.ofNullable(item.getUnitCost()).orElse(BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(Math.max(quantity(item), 0L))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int activePonds(TenantAssistantSnapshot snapshot) {
        return (int) snapshot.ponds().stream().filter(pond -> pond.getStatus() == FishPondStatus.ACTIVE).count();
    }

    private int maintenancePonds(TenantAssistantSnapshot snapshot) {
        return (int) snapshot.ponds().stream().filter(pond -> pond.getStatus() == FishPondStatus.MAINTENANCE).count();
    }

    private int emptyPonds(TenantAssistantSnapshot snapshot) {
        return (int) snapshot.ponds().stream().filter(pond -> pond.getStatus() == FishPondStatus.EMPTY).count();
    }

    private long totalFishStock(TenantAssistantSnapshot snapshot) {
        return snapshot.ponds().stream()
                .mapToLong(pond -> Optional.ofNullable(pond.getCurrentStock()).orElse(0))
                .sum();
    }

    private long totalPondMortality(TenantAssistantSnapshot snapshot) {
        return snapshot.ponds().stream()
                .mapToLong(pond -> Optional.ofNullable(pond.getMortalityCount()).orElse(0))
                .sum();
    }

    private List<FishPond> topMortalityPonds(TenantAssistantSnapshot snapshot) {
        return snapshot.ponds().stream()
                .filter(pond -> Optional.ofNullable(pond.getMortalityCount()).orElse(0) > 0)
                .sorted(Comparator.comparingInt((FishPond pond) -> Optional.ofNullable(pond.getMortalityCount()).orElse(0)).reversed())
                .toList();
    }

    private List<FishPond> staleWaterChangePonds(TenantAssistantSnapshot snapshot) {
        LocalDate threshold = LocalDate.now().minusDays(7);
        return snapshot.ponds().stream()
                .filter(pond -> pond.getStatus() == FishPondStatus.ACTIVE)
                .filter(pond -> pond.getLastWaterChange() == null || pond.getLastWaterChange().isBefore(threshold))
                .toList();
    }

    private long totalLivestockStock(TenantAssistantSnapshot snapshot) {
        return snapshot.livestockGroups().stream()
                .mapToLong(group -> Optional.ofNullable(group.getCurrentStock()).orElse(0))
                .sum();
    }

    private long totalLivestockMortality(TenantAssistantSnapshot snapshot) {
        return snapshot.livestockGroups().stream()
                .mapToLong(group -> Optional.ofNullable(group.getMortality()).orElse(0))
                .sum();
    }

    private Livestock largestLivestockGroup(TenantAssistantSnapshot snapshot) {
        return snapshot.livestockGroups().stream()
                .max(Comparator.comparingInt(group -> Optional.ofNullable(group.getCurrentStock()).orElse(0)))
                .orElse(null);
    }

    private int totalFeedUsed(List<Feed> feeds) {
        return feeds.stream()
                .mapToInt(feed -> Optional.ofNullable(feed.getQuantityUsed()).orElse(0))
                .sum();
    }

    private BigDecimal sumSales(List<Sales> sales) {
        return sales.stream()
                .map(sale -> Optional.ofNullable(sale.getTotalPrice()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal salesForDate(List<Sales> sales, LocalDate date) {
        return sales.stream()
                .filter(sale -> date.equals(sale.getSalesDate()))
                .map(sale -> Optional.ofNullable(sale.getTotalPrice()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Task> overdueTasks(TenantAssistantSnapshot snapshot) {
        LocalDateTime now = LocalDateTime.now();
        return snapshot.pendingTasks().stream()
                .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(now))
                .toList();
    }

    private List<Task> dueSoonTasks(TenantAssistantSnapshot snapshot) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusDays(2);
        return snapshot.pendingTasks().stream()
                .filter(task -> task.getDueDate() != null)
                .filter(task -> !task.getDueDate().isBefore(now) && !task.getDueDate().isAfter(threshold))
                .toList();
    }

    private long unresolvedTicketCount(TenantAssistantSnapshot snapshot) {
        return snapshot.tickets().stream()
                .filter(ticket -> ticket.getStatus() != SupportTicketStatus.RESOLVED)
                .count();
    }

    private String generateTicketCode(Long tenantId) {
        return "TKT-" + tenantId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String generateSupportAck(String subject) {
        String safeSubject = normalizeText(subject);
        String context = safeSubject.isBlank() ? "with your request" : "about \"" + safeSubject + "\"";
        return "Thanks for contacting KFarms support. We have logged your ticket " + context + " and will respond with next steps shortly.";
    }

    private String resolveActorName(String actor) {
        String normalized = normalizeText(actor);
        return normalized.isBlank() ? "Farmer" : normalized;
    }

    private String resolveUserName(SupportAssistantChatRequestDto request, String actor) {
        if (request != null && request.getContext() != null) {
            Object userName = request.getContext().get("userName");
            if (userName instanceof String text && !text.isBlank()) {
                return text.trim();
            }
        }
        return resolveActorName(actor);
    }

    private SupportTicketPriority parsePriority(String value) {
        try {
            return SupportTicketPriority.valueOf(normalizeText(value).toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid support ticket priority.");
        }
    }

    private SupportTicketStatus parseStatus(String value) {
        String normalized = normalizeText(value).toUpperCase(Locale.ROOT);
        if ("CLOSED".equals(normalized)) {
            return SupportTicketStatus.RESOLVED;
        }
        try {
            return SupportTicketStatus.valueOf(normalized);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid support ticket status.");
        }
    }

    private Optional<Long> parseLong(String value) {
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private String resolveSupportCategory(String text, AssistantIntent intent) {
        if (intent == AssistantIntent.BILLING || containsAny(text, "billing", "payment", "subscription", "plan")) {
            return "Billing & Subscription";
        }
        if (intent == AssistantIntent.PONDS || containsAny(text, "pond", "fish", "oxygen", "water")) {
            return "Fish Ponds";
        }
        if (intent == AssistantIntent.FEEDS || containsAny(text, "feed", "nutrition", "biomass")) {
            return "Feeds & Nutrition";
        }
        if (intent == AssistantIntent.INVENTORY || containsAny(text, "inventory", "stock", "reorder", "suppl")) {
            return "Inventory & Supplies";
        }
        if (intent == AssistantIntent.SALES || containsAny(text, "sales", "revenue", "buyer")) {
            return "Sales & Revenue";
        }
        if (intent == AssistantIntent.LIVESTOCK || containsAny(text, "livestock", "broiler", "layer", "noiler")) {
            return "Livestock";
        }
        if (intent == AssistantIntent.TEAM || containsAny(text, "team", "workspace", "member", "role", "staff")) {
            return "Workspace & Access";
        }
        return "General";
    }

    private String resolveSupportPriority(String text) {
        if (containsAny(text, "critical", "urgent", "mortality", "disease", "outbreak", "failed payment")) {
            return "HIGH";
        }
        if (containsAny(text, "error", "wrong", "broken", "stockout", "stuck")) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private Map<String, Object> toTicketPayload(SupportTicket ticket) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ticketId", ticket.getTicketCode());
        payload.put("subject", ticket.getSubject());
        payload.put("category", ticket.getCategory());
        payload.put("priority", ticket.getPriority() != null ? ticket.getPriority().name() : SupportTicketPriority.MEDIUM.name());
        payload.put("status", ticket.getStatus() != null ? ticket.getStatus().name() : SupportTicketStatus.OPEN.name());
        payload.put("lane", resolveSupportLane(ticket));
        payload.put("plan", ticket.getTenant() != null && ticket.getTenant().getPlan() != null
                ? ticket.getTenant().getPlan().name()
                : TenantPlan.FREE.name());
        payload.put("description", ticket.getDescription());
        payload.put("createdAt", ticket.getCreatedAt());
        payload.put("updatedAt", ticket.getUpdatedAt());
        payload.put(
                "messages",
                ticket.getMessages().stream()
                        .map(this::toTicketMessagePayload)
                        .toList()
        );
        return payload;
    }

    private Map<String, Object> toPlatformTicketPayload(SupportTicket ticket) {
        Map<String, Object> payload = toTicketPayload(ticket);
        Tenant tenant = ticket.getTenant();
        payload.put("tenantId", tenant != null ? tenant.getId() : null);
        payload.put("tenantName", tenant != null ? tenant.getName() : "");
        payload.put("tenantSlug", tenant != null ? tenant.getSlug() : "");
        payload.put("tenantPlan", tenant != null && tenant.getPlan() != null ? tenant.getPlan().name() : TenantPlan.FREE.name());
        payload.put("tenantStatus", tenant != null && tenant.getStatus() != null ? tenant.getStatus().name() : "");
        payload.put("tenantContactEmail", tenant != null ? normalizeText(tenant.getContactEmail()) : "");
        payload.put("laneLabel", resolveSupportLaneLabel(ticket));
        return payload;
    }

    private boolean matchesPlatformSearch(SupportTicket ticket, String normalizedSearch) {
        if (normalizedSearch.isBlank()) {
            return true;
        }

        Tenant tenant = ticket.getTenant();
        String haystack = String.join(
                " ",
                normalizeText(ticket.getTicketCode()),
                normalizeText(ticket.getSubject()),
                normalizeText(ticket.getCategory()),
                normalizeText(ticket.getDescription()),
                tenant != null ? normalizeText(tenant.getName()) : "",
                tenant != null ? normalizeText(tenant.getSlug()) : "",
                tenant != null ? normalizeText(tenant.getContactEmail()) : ""
        ).toLowerCase(Locale.ROOT);

        return haystack.contains(normalizedSearch);
    }

    private boolean matchesPlatformStatus(SupportTicket ticket, String normalizedStatus) {
        if (normalizedStatus.isBlank() || "ALL".equals(normalizedStatus)) {
            return true;
        }
        String currentStatus = ticket.getStatus() != null ? ticket.getStatus().name() : SupportTicketStatus.OPEN.name();
        return currentStatus.equalsIgnoreCase(normalizedStatus);
    }

    private boolean matchesPlatformLane(SupportTicket ticket, String normalizedLane) {
        if (normalizedLane.isBlank() || "ALL".equals(normalizedLane)) {
            return true;
        }
        return resolveSupportLane(ticket).equalsIgnoreCase(normalizedLane);
    }

    private Map<String, Object> summarizeTicketsByStatus(List<Map<String, Object>> items) {
        Map<String, Long> counts = items.stream()
                .collect(Collectors.groupingBy(
                        (item) -> String.valueOf(item.getOrDefault("status", SupportTicketStatus.OPEN.name())).toUpperCase(Locale.ROOT),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("OPEN", counts.getOrDefault("OPEN", 0L));
        payload.put("PENDING", counts.getOrDefault("PENDING", 0L));
        payload.put("RESOLVED", counts.getOrDefault("RESOLVED", 0L));
        return payload;
    }

    private Map<String, Object> summarizeTicketsByLane(List<Map<String, Object>> items) {
        Map<String, Long> counts = items.stream()
                .collect(Collectors.groupingBy(
                        (item) -> String.valueOf(item.getOrDefault("lane", "STANDARD")).toUpperCase(Locale.ROOT),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("STANDARD", counts.getOrDefault("STANDARD", 0L));
        payload.put("PRIORITY", counts.getOrDefault("PRIORITY", 0L));
        payload.put("DEDICATED", counts.getOrDefault("DEDICATED", 0L));
        return payload;
    }

    private String resolveSupportLane(SupportTicket ticket) {
        TenantPlan plan = ticket.getTenant() != null && ticket.getTenant().getPlan() != null
                ? ticket.getTenant().getPlan()
                : TenantPlan.FREE;
        if (plan == TenantPlan.ENTERPRISE) {
            return "DEDICATED";
        }
        if (plan == TenantPlan.PRO) {
            return "PRIORITY";
        }
        return "STANDARD";
    }

    private String resolveSupportLaneLabel(SupportTicket ticket) {
        return switch (resolveSupportLane(ticket)) {
            case "DEDICATED" -> "Dedicated lane";
            case "PRIORITY" -> "Priority lane";
            default -> "Standard lane";
        };
    }

    private Map<String, Object> toTicketMessagePayload(SupportTicketMessage message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", message.getId() != null ? "MSG-" + message.getId() : UUID.randomUUID().toString());
        payload.put("body", message.getBody());
        payload.put("authorType", message.getAuthorType() != null ? message.getAuthorType().name() : SupportMessageAuthorType.USER.name());
        payload.put("authorName", message.getAuthorName());
        payload.put("createdAt", message.getCreatedAt());
        return payload;
    }

    private Map<String, Object> toAssistantMessagePayload(SupportAssistantMessage message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", message.getId() != null ? "AST-" + message.getId() : UUID.randomUUID().toString());
        payload.put("role", message.getRole() != null ? message.getRole().name().toLowerCase(Locale.ROOT) : "assistant");
        payload.put("content", message.getContent());
        payload.put("createdAt", message.getCreatedAt());
        return payload;
    }

    private List<String> uniqueSuggestions(List<String> suggestions) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String suggestion : suggestions) {
            String normalized = normalizeText(suggestion);
            if (!normalized.isBlank()) {
                unique.add(normalized);
            }
            if (unique.size() >= 3) {
                break;
            }
        }
        return new ArrayList<>(unique);
    }

    private List<Map<String, Object>> limitActions(List<Map<String, Object>> actions) {
        List<Map<String, Object>> limited = new ArrayList<>();
        for (Map<String, Object> action : actions) {
            if (action == null || action.isEmpty()) {
                continue;
            }
            limited.add(action);
            if (limited.size() >= 3) {
                break;
            }
        }
        return limited;
    }

    private String buildSupportTicketTarget(
            String category,
            String priority,
            String subject,
            String description
    ) {
        return "/support?tab=tickets"
                + "&category=" + encode(category)
                + "&priority=" + encode(priority)
                + "&subject=" + encode(truncate(subject, 140))
                + "&description=" + encode(truncate(description, 1000));
    }

    private Map<String, Object> navigateAction(String id, String label, String target) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("id", id);
        action.put("type", "navigate");
        action.put("label", label);
        action.put("target", target);
        return action;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isGreeting(String text) {
        return containsAny(text, "hello", "hi", "hey", "good morning", "good afternoon", "good evening");
    }

    private Optional<Map<String, Object>> findGuideById(String guideId) {
        return FARMER_GUIDES.stream()
                .filter(guide -> guideId.equals(guide.get("id")))
                .findFirst();
    }

    private int quantity(Inventory item) {
        return Optional.ofNullable(item.getQuantity()).orElse(0);
    }

    private int minThreshold(Inventory item) {
        return Math.max(item.getMinThreshold(), 0);
    }

    private String formatInventoryItem(Inventory item) {
        String unit = normalizeText(item.getUnit());
        return item.getItemName()
                + " (" + formatNumber(quantity(item)) + (unit.isBlank() ? "" : " " + unit)
                + ", min " + formatNumber(minThreshold(item)) + (unit.isBlank() ? "" : " " + unit) + ")";
    }

    private String formatFeedEntry(Feed feed) {
        return feed.getFeedName()
                + " - "
                + formatNumber(Optional.ofNullable(feed.getQuantityUsed()).orElse(0))
                + " on "
                + formatDate(feed.getDate());
    }

    private String formatPondMortality(FishPond pond) {
        return pond.getPondName() + " - " + formatNumber(Optional.ofNullable(pond.getMortalityCount()).orElse(0)) + " mortality";
    }

    private String formatWaterAttention(FishPond pond) {
        if (pond.getLastWaterChange() == null) {
            return pond.getPondName() + " - no water-change date recorded";
        }
        return pond.getPondName() + " - last water change " + formatDate(pond.getLastWaterChange());
    }

    private String formatSale(Sales sale) {
        String buyer = normalizeText(sale.getBuyer()).isBlank() ? "Walk-in customer" : normalizeText(sale.getBuyer());
        return sale.getItemName()
                + " to "
                + buyer
                + " on "
                + formatDate(sale.getSalesDate())
                + " for "
                + formatCurrency(Optional.ofNullable(sale.getTotalPrice()).orElse(BigDecimal.ZERO));
    }

    private String formatTask(Task task) {
        if (task.getDueDate() == null) {
            return task.getTitle() + " - no due date";
        }
        return task.getTitle() + " - due " + formatDateTime(task.getDueDate());
    }

    private String formatCurrency(BigDecimal amount) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.ENGLISH);
        numberFormat.setMaximumFractionDigits(2);
        numberFormat.setMinimumFractionDigits(amount.scale() > 0 ? 2 : 0);
        return "NGN " + numberFormat.format(amount);
    }

    private String formatNumber(long value) {
        return NumberFormat.getIntegerInstance(Locale.ENGLISH).format(value);
    }

    private String formatDate(LocalDate value) {
        return value == null ? "not recorded" : DATE_FORMAT.format(value);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "not scheduled" : DATE_TIME_FORMAT.format(value);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String truncate(String value, int maxLength) {
        String normalized = normalizeText(value);
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }

    private String encode(String value) {
        return URLEncoder.encode(normalizeText(value), StandardCharsets.UTF_8);
    }

    private String quote(String value) {
        return "\"" + value + "\"";
    }

    private static Map<String, Object> channel(
            String id,
            String label,
            String value,
            String note,
            String href
    ) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("label", label);
        item.put("value", value);
        item.put("note", note);
        item.put("href", href);
        return item;
    }

    private static Map<String, Object> guide(
            String id,
            String category,
            String title,
            String summary,
            List<String> steps,
            String tip
    ) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("category", category);
        item.put("title", title);
        item.put("summary", summary);
        item.put("steps", steps);
        item.put("tip", tip);
        return item;
    }

    private static Map<String, Object> faq(
            String id,
            String question,
            String answer
    ) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("question", question);
        item.put("answer", answer);
        return item;
    }

    private enum AssistantIntent {
        GREETING,
        OVERVIEW,
        INVENTORY,
        FEEDS,
        PONDS,
        LIVESTOCK,
        SALES,
        TASKS,
        SUPPORT,
        BILLING,
        TEAM,
        UNKNOWN
    }

    private record TenantAssistantSnapshot(
            Tenant tenant,
            List<Inventory> inventoryItems,
            List<Feed> recentFeeds,
            List<Feed> weeklyFeeds,
            List<Sales> recentSales,
            List<Sales> weeklySales,
            List<FishPond> ponds,
            List<Livestock> livestockGroups,
            List<Task> pendingTasks,
            List<Notification> unreadNotifications,
            List<SupportTicket> tickets,
            int memberCount,
            BillingSubscription billingSubscription
    ) {
    }

    private record AssistantReply(
            String reply,
            List<String> suggestions,
            List<Map<String, Object>> actions
    ) {
    }
}
