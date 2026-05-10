package com.kfarms.tenant.service;

import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.entity.TenantPlan;
import com.kfarms.tenant.entity.TenantRole;
import com.kfarms.tenant.repository.InvitationRepository;
import com.kfarms.tenant.repository.TenantMemberRepository;
import com.kfarms.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantPlanGuardService {

    private final TenantRepository tenantRepository;
    private final TenantMemberRepository tenantMemberRepository;
    private final InvitationRepository invitationRepository;

    public Tenant requireCurrentTenant() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Missing tenant context.");
        }

        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found."));
    }

    public boolean isAtLeast(TenantPlan currentPlan, TenantPlan requiredPlan) {
        TenantPlan resolvedCurrent = normalizePlan(currentPlan);
        TenantPlan resolvedRequired = normalizePlan(requiredPlan);
        return resolvedCurrent.ordinal() >= resolvedRequired.ordinal();
    }

    public void requirePlanAccess(Tenant tenant, TenantPlan requiredPlan, String message) {
        if (!isAtLeast(tenant != null ? tenant.getPlan() : TenantPlan.FREE, requiredPlan)) {
            throw new AccessDeniedException(message);
        }
    }

    public void requireCurrentTenantPlanAccess(TenantPlan requiredPlan, String message) {
        requirePlanAccess(requireCurrentTenant(), requiredPlan, message);
    }

    public int maxOrganizationsForPlan(TenantPlan plan) {
        return switch (normalizePlan(plan)) {
            case FREE, PRO -> 1;
            case ENTERPRISE -> Integer.MAX_VALUE;
        };
    }

    public int maxUsersForPlan(TenantPlan plan) {
        return switch (normalizePlan(plan)) {
            case FREE -> 2;
            case PRO -> 10;
            case ENTERPRISE -> Integer.MAX_VALUE;
        };
    }

    public int maxFishPondsForPlan(TenantPlan plan) {
        return switch (normalizePlan(plan)) {
            case FREE -> 5;
            case PRO, ENTERPRISE -> Integer.MAX_VALUE;
        };
    }

    public int maxPoultryFlocksForPlan(TenantPlan plan) {
        return switch (normalizePlan(plan)) {
            case FREE -> 3;
            case PRO, ENTERPRISE -> Integer.MAX_VALUE;
        };
    }

    public TenantPlan highestOwnedPlanForUser(Long userId) {
        return tenantMemberRepository.findAllActiveWithTenant(userId).stream()
                .filter(member -> member.getRole() == TenantRole.OWNER)
                .map(member -> normalizePlan(member.getTenant().getPlan()))
                .max(Enum::compareTo)
                .orElse(TenantPlan.FREE);
    }

    public void ensureOrganizationCapacity(TenantPlan plan, long currentOwnedCount) {
        int maxOrganizations = maxOrganizationsForPlan(plan);
        if (maxOrganizations != Integer.MAX_VALUE && currentOwnedCount >= maxOrganizations) {
            throw new IllegalArgumentException(
                    "Organization limit reached for your plan (" + normalizePlan(plan).name() + ")."
            );
        }
    }

    public void ensureSeatCapacityForInvite(Tenant tenant) {
        int maxUsers = maxUsersForPlan(tenant.getPlan());
        if (maxUsers == Integer.MAX_VALUE) {
            return;
        }

        int activeMembers = tenantMemberRepository.countByTenant_IdAndActiveTrue(tenant.getId());
        int pendingInvites = invitationRepository.countByTenant_IdAndAcceptedFalse(tenant.getId());
        if (activeMembers + pendingInvites >= maxUsers) {
            throw new IllegalArgumentException(
                    "User limit reached for the " + normalizePlan(tenant.getPlan()).name() + " plan."
            );
        }
    }

    public void ensureSeatCapacityForActivation(Tenant tenant) {
        int maxUsers = maxUsersForPlan(tenant.getPlan());
        if (maxUsers == Integer.MAX_VALUE) {
            return;
        }

        int activeMembers = tenantMemberRepository.countByTenant_IdAndActiveTrue(tenant.getId());
        if (activeMembers >= maxUsers) {
            throw new IllegalArgumentException(
                    "User limit reached for the " + normalizePlan(tenant.getPlan()).name() + " plan."
            );
        }
    }

    private TenantPlan normalizePlan(TenantPlan plan) {
        return plan == null ? TenantPlan.FREE : plan;
    }
}
