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
            Pageable pageable
    ) {

        Specification<AppUser> spec = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

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

        AppUser user = appUserRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "userId", userId));

        user.setRole(value ? Role.PLATFORM_ADMIN : Role.USER);
        appUserRepo.save(user);
    }

    @Override
    public void setUserEnabled(Long userId, boolean value) {
        AppUser user = appUserRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "userId", userId));

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
                .active(user.isEnabled())
                .tenantCount(tenantCount)
                .createdAt(user.getCreatedAt())
                .build();
    }


}
