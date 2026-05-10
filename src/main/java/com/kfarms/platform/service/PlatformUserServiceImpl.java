package com.kfarms.platform.service;

import com.kfarms.entity.AppUser;
import com.kfarms.entity.Role;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.platform.dto.PlatformUserListItemDto;
import com.kfarms.repository.AppUserRepository;
import com.kfarms.tenant.repository.TenantMemberRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class PlatformUserServiceImpl implements PlatformUserService {

    private final AppUserRepository appUserRepo;
    private final TenantMemberRepository tenantMemberRepo;

    @Override
    public Page<PlatformUserListItemDto> searchUsers(
            String search,
            boolean platformOnly,
            Pageable pageable
    ) {

        Specification<AppUser> spec = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (platformOnly) {
                predicates.add(
                        cb.or(
                                cb.isTrue(root.get("platformAccess")),
                                cb.equal(root.get("role"), Role.PLATFORM_ADMIN)
                        )
                );
            }

            // search filter (username or email)
            if (search != null && !search.trim().isEmpty()) {

                String like = "%" + search.trim().toLowerCase() + "%";

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("username")), like),
                                cb.like(cb.lower(root.get("email")), like)
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AppUser> users = appUserRepo.findAll(spec, pageable);

        return users.map(this::toDto);
    }

    @Override
    public void setPlatformAdmin(Long userId, boolean value) {
        AppUser actor = requireCurrentActor();

        AppUser user = appUserRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "userId", userId));

        ensureNotSelf(actor, user, "Use another platform admin account to change your own platform role.");
        ensurePlatformAdminCoverage(user, value, user.isEnabled());

        user.setRole(value ? Role.PLATFORM_ADMIN : Role.USER);
        if (value) {
            user.setPlatformAccess(true);
        }
        appUserRepo.save(user);
    }

    @Override
    public void setUserEnabled(Long userId, boolean value) {
        AppUser actor = requireCurrentActor();

        AppUser user = appUserRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "userId", userId));

        ensureNotSelf(actor, user, "Use another platform admin account to change your own sign-in access.");
        ensurePlatformAdminCoverage(user, user.getRole(), value);

        user.setEnabled(value);

        appUserRepo.save(user);
    }

    private PlatformUserListItemDto toDto(AppUser user) {

        int tenantCount = tenantMemberRepo.countByUser_Id(user.getId());

        // If enabled/createdAt not available, remove them
        return PlatformUserListItemDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .platformAccess(user.isPlatformAccess() || user.getRole() == Role.PLATFORM_ADMIN)
                .active(user.isEnabled())
                .tenantCount(tenantCount)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private AppUser requireCurrentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("User not authenticated.");
        }

        String principal = authentication.getName();
        return appUserRepo.findByEmail(principal)
                .or(() -> appUserRepo.findByUsername(principal))
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private void ensureNotSelf(AppUser actor, AppUser target, String message) {
        if (actor != null && actor.getId() != null && actor.getId().equals(target.getId())) {
            throw new IllegalArgumentException(message);
        }
    }

    private void ensurePlatformAdminCoverage(AppUser target, boolean nextPlatformAdmin, boolean nextEnabled) {
        ensurePlatformAdminCoverage(target, nextPlatformAdmin ? Role.PLATFORM_ADMIN : Role.USER, nextEnabled);
    }

    private void ensurePlatformAdminCoverage(AppUser target, Role nextRole, boolean nextEnabled) {
        boolean currentlyEnabledPlatformAdmin =
                target.getRole() == Role.PLATFORM_ADMIN && target.isEnabled();
        boolean remainsEnabledPlatformAdmin =
                nextRole == Role.PLATFORM_ADMIN && nextEnabled;

        if (!currentlyEnabledPlatformAdmin || remainsEnabledPlatformAdmin) {
            return;
        }

        long enabledPlatformAdmins = appUserRepo.countByRoleAndEnabledTrue(Role.PLATFORM_ADMIN);
        if (enabledPlatformAdmins <= 1) {
            throw new IllegalArgumentException("At least one enabled platform admin must remain.");
        }
    }


}
