package com.kfarms.platform.service;


import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.platform.dto.TenantAdminDetailsDto;
import com.kfarms.platform.dto.TenantAdminListItemDto;
import com.kfarms.platform.dto.TenantMemberSummaryDto;
import com.kfarms.repository.FeedRepository;
import com.kfarms.repository.FishPondRepository;
import com.kfarms.repository.LivestockRepository;
import com.kfarms.repository.SalesRepository;
import com.kfarms.service.NotificationService;
import com.kfarms.tenant.entity.*;
import com.kfarms.tenant.repository.TenantMemberRepository;
import com.kfarms.tenant.repository.TenantRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlatformTenantServiceImpl implements PlatformTenantService {

    private static final String CURRENT_APP_ID = "kfarms";
    private static final String CURRENT_APP_NAME = "KFarms";
    private static final String CURRENT_APP_LIFECYCLE = "LIVE";

    private final TenantRepository tenantRepository;
    private final TenantMemberRepository tenantMemberRepository;
    private final LivestockRepository livestockRepository;
    private final FishPondRepository fishPondRepository;
    private final FeedRepository feedRepository;
    private final SalesRepository saleRepository;
    private final NotificationService notificationService; // optional for informing owners

    @Override
    @Transactional(readOnly = true)
    public Page<TenantAdminListItemDto> searchTenants(
            String search,
            TenantStatus status,
            TenantPlan plan,
            Pageable pageable
    ) {
        Specification<Tenant> spec = (root, query, cb) -> {

            if (query != null) {
                query.distinct(true); // VERY IMPORTANT to avoid duplicates
            }

            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (plan != null) {
                predicates.add(cb.equal(root.get("plan"), plan));
            }

            if (search != null && !search.trim().isEmpty()) {

                String like = "%" + search.trim().toLowerCase() + "%";

                // Join Tenant → TenantMember → AppUser
                var memberJoin = root.join("members", jakarta.persistence.criteria.JoinType.LEFT);
                var userJoin = memberJoin.join("user", jakarta.persistence.criteria.JoinType.LEFT);

                Predicate nameMatch =
                        cb.like(cb.lower(root.get("name")), like);

                Predicate slugMatch =
                        cb.like(cb.lower(root.get("slug")), like);

                Predicate ownerEmailMatch =
                        cb.and(
                                cb.equal(memberJoin.get("role"), TenantRole.OWNER),
                                cb.like(cb.lower(userJoin.get("email")), like)
                        );

                predicates.add(
                        cb.or(nameMatch, slugMatch, ownerEmailMatch)
                );
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };


        Page<Tenant> tenants = tenantRepository.findAll(spec, pageable);

        return tenants.map(this::mapToListDto);

    }

    @Override
    @Transactional(readOnly = true)
    public TenantAdminDetailsDto getTenantDetails(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found", "tenantId", tenantId));

        List<TenantMember> members = tenantMemberRepository.findAllByTenantIdWithUser(tenantId);

        long livestockCount = livestockRepository.countByTenantId(tenantId);
        long fishPondCount = fishPondRepository.countByTenantId(tenantId);
        long feedItemCount = feedRepository.countByTenantId(tenantId);
        long salesCount = saleRepository.countByTenantId(tenantId);

        return mapToDetailsDto(tenant, members, livestockCount, fishPondCount,feedItemCount, salesCount);
    }

    @Override
    @Transactional
    public void updateTenantPlan(Long tenantId, TenantPlan newPlan) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found", "tenantId", tenantId));

        TenantPlan oldPlan = tenant.getPlan();
        if (oldPlan == newPlan) {
            return;
        }

        tenant.setPlan(newPlan);
        tenantRepository.save(tenant);

        notifyTenantOwnersAndAdmins(tenant, "Your plan has been changed from " + oldPlan + " to " + newPlan);
    }

    @Override
    @Transactional
    public void updateTenantStatus(Long tenantId, TenantStatus newStatus) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found", "tenantId", tenantId));


        TenantStatus oldStatus = tenant.getStatus();
        if (oldStatus == newStatus) {
            return;
        }

        tenant.setStatus(newStatus);
        tenantRepository.save(tenant);

        notifyTenantOwnersAndAdmins(tenant, "Your tenant status has been changed to " + newStatus);
    }

    private TenantAdminListItemDto mapToListDto(Tenant tenant) {

        TenantMember owner = tenantMemberRepository
                .findFirstByTenantIdAndRoleWithUser(tenant.getId(), TenantRole.OWNER)
                .stream()
                .findFirst()
                .orElse(null);

        int memberCount = tenantMemberRepository.countByTenant_Id(tenant.getId());

        return TenantAdminListItemDto.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .plan(tenant.getPlan())
                .status(tenant.getStatus())
                .appId(CURRENT_APP_ID)
                .appName(CURRENT_APP_NAME)
                .appLifecycle(CURRENT_APP_LIFECYCLE)
                .ownerEmail(owner != null ? owner.getUser().getEmail() : null)
                .memberCount(memberCount)
                .createdAt(tenant.getCreatedAt())
                .lastActivityAt(tenant.getUpdatedAt()) // or from audit logs later
        .build();
    }

    private TenantAdminDetailsDto mapToDetailsDto(
            Tenant tenant,
            List<TenantMember> members,
            long livestockCount,
            long fishPondCount,
            long feedItemCount,
            long salesCount
    ) {
        List<TenantMemberSummaryDto> memberDtos = members.stream()
                .map(m -> TenantMemberSummaryDto.builder()
                        .id(m.getId())
                        .email(m.getUser().getEmail())
                        .fullName(m.getUser().getUsername()) // or firstName/lastName if you have
                        .role(m.getRole().name())
                        .active(m.isActive())
                        .build())
                .toList();

        TenantMember owner = members.stream()
                .filter(m -> m.getRole() == TenantRole.OWNER)
                .findFirst().orElse(null);

        return TenantAdminDetailsDto.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .plan(tenant.getPlan())
                .status(tenant.getStatus())
                .appId(CURRENT_APP_ID)
                .appName(CURRENT_APP_NAME)
                .appLifecycle(CURRENT_APP_LIFECYCLE)
                .ownerEmail(owner != null ? owner.getUser().getEmail() : null)
                .memberCount(members.size())
                .createdAt(tenant.getCreatedAt())
                .lastActivityAt(tenant.getUpdatedAt())
                .livestockCount(livestockCount)
                .fishPondCount(fishPondCount)
                .feedItemCount(feedItemCount)
                .salesCount(salesCount)
                .enabledModules(resolveEnabledModules(tenant))
                .members(memberDtos)
                .build();
    }

    private List<String> resolveEnabledModules(Tenant tenant) {
        List<String> modules = new ArrayList<>();

        if (Boolean.TRUE.equals(tenant.getPoultryEnabled())) {
            modules.add("POULTRY");
        }

        if (Boolean.TRUE.equals(tenant.getFishEnabled())) {
            modules.add("FISH_FARMING");
        }

        return modules;
    }


    private void notifyTenantOwnersAndAdmins(Tenant tenant, String message) {

        List<TenantMember> ownersAndAdmins =
                tenantMemberRepository.findActiveMembersByTenantAndRoles(
                        tenant.getId(),
                        List.of(TenantRole.OWNER, TenantRole.ADMIN)
                );

        for (TenantMember member : ownersAndAdmins) {

            notificationService.createNotification(
                    tenant.getId(),
                    "TENANT_UPDATE",
                    "Tenant update",
                    message,
                    member.getUser()
            );
        }
    }
}
