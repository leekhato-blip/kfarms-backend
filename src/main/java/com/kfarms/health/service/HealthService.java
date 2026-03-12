package com.kfarms.health.service;

import com.kfarms.health.dto.AdviceContext;
import com.kfarms.health.entity.HealthEvent;
import com.kfarms.health.entity.HealthRule;
import com.kfarms.health.enums.HealthEventStatus;
import com.kfarms.health.repo.HealthEventRepo;
import com.kfarms.health.repo.HealthRuleRepo;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.entity.TenantPlan;
import com.kfarms.tenant.repository.TenantRepository;
import com.kfarms.tenant.service.TenantContext;
import com.kfarms.tenant.service.TenantPlanGuardService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class HealthService {

    private static final List<HealthEventStatus> ACTIVE_STATUSES = List.of(
            HealthEventStatus.NEW,
            HealthEventStatus.ACKNOWLEDGED
    );

    private final HealthRuleRepo ruleRepo;
    private final HealthEventRepo eventRepo;
    private final HealthAdviceService adviceService;
    private final TenantRepository tenantRepository;
    private final TenantPlanGuardService tenantPlanGuardService;

    public HealthService(
            HealthRuleRepo ruleRepo,
            HealthEventRepo eventRepo,
            HealthAdviceService adviceService,
            TenantRepository tenantRepository,
            TenantPlanGuardService tenantPlanGuardService
    ) {
        this.ruleRepo = ruleRepo;
        this.eventRepo = eventRepo;
        this.adviceService = adviceService;
        this.tenantRepository = tenantRepository;
        this.tenantPlanGuardService = tenantPlanGuardService;
    }

    /**
     * SINGLE SOURCE OF TRUTH for triggering health rules
     */
    public HealthEvent triggerRuleByCode(
            String code,
            String contextNote,
            String season
    ) {
        return triggerRuleByCode(code, contextNote, season, null);
    }

    public HealthEvent triggerRuleByCode(
            String code,
            String contextNote,
            String season,
            String sourceKey
    ) {
        Tenant tenant = requireHealthAlertsTenant();
        return triggerRuleByCode(code, contextNote, season, sourceKey, tenant);
    }

    public HealthEvent triggerRuleByCode(
            String code,
            String contextNote,
            String season,
            String sourceKey,
            Tenant tenant
    ) {
        if (tenant == null || tenant.getId() == null) {
            throw new IllegalArgumentException("Tenant is required for health alerts.");
        }
        if (!tenantPlanGuardService.isAtLeast(tenant.getPlan(), TenantPlan.PRO)) {
            return null;
        }

        HealthRule rule = ruleRepo.findByCodeIgnoreCase(code)
                .orElseThrow(() ->
                        new RuntimeException("Rule not found " + code)
                );
        if (!Boolean.TRUE.equals(rule.getActive())) {
            throw new IllegalArgumentException("Health rule is disabled: " + code);
        }

        String resolvedSourceKey = normalizeSourceKey(code, sourceKey);
        boolean activeAlertExists = eventRepo.existsByTenant_IdAndRuleAndStatusInAndSourceKeyIgnoreCase(
                tenant.getId(),
                rule,
                ACTIVE_STATUSES,
                resolvedSourceKey
        );

        if (activeAlertExists){
            return null;
        }

        if (rule.getCooldownHours() != null && rule.getCooldownHours() > 0) {
            LocalDateTime limit =
                    LocalDateTime.now().minusHours(rule.getCooldownHours());
            boolean exists = eventRepo.existsByTenant_IdAndRuleAndSourceKeyIgnoreCaseAndTriggeredAtAfter(
                    tenant.getId(),
                    rule,
                    resolvedSourceKey,
                    limit
            );
            if (exists) {
                return null;
            }
        }

        HealthEvent event = new HealthEvent();
        event.setTenant(tenant);
        event.setRule(rule);
        event.setSeverity(rule.getSeverity());
        event.setTriggeredAt(LocalDateTime.now());
        event.setContextNote(contextNote);
        event.setSourceKey(resolvedSourceKey);

        AdviceContext adviceContext = new AdviceContext();
        adviceContext.setRuleCode(rule.getCode());
        adviceContext.setRuleTitle(rule.getTitle());
        adviceContext.setContextNote(contextNote);
        adviceContext.setSeason(season);
        adviceContext.setLivestockType("layers, turkeys, ducks, fish");
        List<String> adviceSteps = adviceService.generateAdvice(adviceContext);

        event.setAdviceSteps(adviceSteps);

        return eventRepo.save(event);
    }

    @Transactional(readOnly = true)
    public List<HealthEvent> getActiveEvents() {
        Tenant tenant = requireHealthAlertsTenant();
        return eventRepo.findByTenant_IdAndStatusInOrderByTriggeredAtDesc(tenant.getId(), ACTIVE_STATUSES);
    }

    public HealthEvent acknowledgeEvent(Long eventId) {
        Tenant tenant = requireHealthAlertsTenant();
        HealthEvent event = requireTenantEvent(eventId, tenant.getId());
        if (event.getStatus() == HealthEventStatus.HANDLED || event.getStatus() == HealthEventStatus.EXPIRED) {
            throw new IllegalArgumentException("This health alert can no longer be acknowledged.");
        }
        if (event.getStatus() == HealthEventStatus.ACKNOWLEDGED) {
            return event;
        }

        event.setStatus(HealthEventStatus.ACKNOWLEDGED);
        event.setAcknowledgedAt(LocalDateTime.now());
        return eventRepo.save(event);
    }

    public HealthEvent handleEvent(Long eventId) {
        Tenant tenant = requireHealthAlertsTenant();
        HealthEvent event = requireTenantEvent(eventId, tenant.getId());
        if (event.getStatus() == HealthEventStatus.EXPIRED) {
            throw new IllegalArgumentException("Expired health alerts cannot be handled.");
        }
        if (event.getStatus() == HealthEventStatus.HANDLED) {
            return event;
        }

        if (event.getAcknowledgedAt() == null) {
            event.setAcknowledgedAt(LocalDateTime.now());
        }
        event.setStatus(HealthEventStatus.HANDLED);
        event.setHandledAt(LocalDateTime.now());
        return eventRepo.save(event);
    }

    private Tenant requireTenant() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Missing tenant context.");
        }

        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found."));
    }

    private Tenant requireHealthAlertsTenant() {
        Tenant tenant = requireTenant();
        if (!tenantPlanGuardService.isAtLeast(tenant.getPlan(), TenantPlan.PRO)) {
            throw new AccessDeniedException(
                    "Health alerts are available on Pro and Enterprise plans."
            );
        }
        return tenant;
    }

    private HealthEvent requireTenantEvent(Long eventId, Long tenantId) {
        return eventRepo.findByIdAndTenant_Id(eventId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Health alert not found."));
    }

    private String normalizeSourceKey(String code, String sourceKey) {
        String raw = sourceKey == null || sourceKey.isBlank()
                ? "rule:" + code
                : sourceKey.trim();
        return raw.toLowerCase(Locale.ROOT);
    }
}
