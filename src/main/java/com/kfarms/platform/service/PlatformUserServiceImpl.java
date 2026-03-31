package com.kfarms.platform.service;

import com.kfarms.dto.LoginResponse;
import com.kfarms.dto.UserDto;
import com.kfarms.entity.AppUser;
import com.kfarms.entity.Role;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.platform.dto.AcceptPlatformInviteRequest;
import com.kfarms.platform.dto.CreatePlatformInviteRequest;
import com.kfarms.platform.dto.CreatePlatformUserRequest;
import com.kfarms.platform.dto.PlatformInvitePreviewDto;
import com.kfarms.platform.dto.PlatformUserInviteDto;
import com.kfarms.platform.dto.PlatformUserListItemDto;
import com.kfarms.platform.entity.PlatformUserInvitation;
import com.kfarms.platform.repository.PlatformUserInvitationRepository;
import com.kfarms.repository.AppUserRepository;
import com.kfarms.security.JwtService;
import com.kfarms.tenant.repository.TenantMemberRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlatformUserServiceImpl implements PlatformUserService {

    private static final String TOKEN_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();

    private final AppUserRepository appUserRepo;
    private final TenantMemberRepository tenantMemberRepo;
    private final PlatformUserInvitationRepository invitationRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public Page<PlatformUserListItemDto> searchUsers(
            String search,
            boolean platformOnly,
            Pageable pageable
    ) {
        String normalizedSearch = StringUtils.hasText(search) ? search.trim().toLowerCase() : null;

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

            if (normalizedSearch != null) {
                String like = "%" + normalizedSearch + "%";
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
        List<PlatformUserListItemDto> items = new ArrayList<>(users.getNumberOfElements());
        for (AppUser user : users.getContent()) {
            items.add(buildUserListItem(user));
        }

        return new PageImpl<>(items, pageable, users.getTotalElements());
    }

    @Override
    public PlatformUserListItemDto createPlatformUser(CreatePlatformUserRequest request) {
        String username = normalizeUsername(request.getUsername());
        String normalizedEmail = normalizeEmail(request.getEmail());
        String password = request.getPassword();

        if (!StringUtils.hasText(password) || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }

        ensureIdentityAvailable(username, normalizedEmail);

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.PLATFORM_ADMIN);
        user.setPlatformAccess(true);
        user.setEnabled(request.getEnabled() == null || request.getEnabled());

        return buildUserListItem(appUserRepo.save(user));
    }

    @Override
    @Transactional
    public PlatformUserInviteDto createPlatformInvite(
            CreatePlatformInviteRequest request,
            String inviterIdentity
    ) {
        String username = normalizeUsername(request.getUsername());
        String normalizedEmail = normalizeEmail(request.getEmail());
        ensureIdentityAvailable(username, normalizedEmail);

        PlatformUserInvitation invitation = findReusableInvitation(username, normalizedEmail)
                .orElseGet(PlatformUserInvitation::new);

        invitation.setUsername(username);
        invitation.setEmail(normalizedEmail);
        invitation.setToken(randomToken(48));
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        invitation.setAccepted(false);
        invitation.setDeleted(false);
        invitation.setDeletedAt(null);
        if (StringUtils.hasText(inviterIdentity)) {
            invitation.setCreatedBy(inviterIdentity);
            invitation.setUpdatedBy(inviterIdentity);
        }

        PlatformUserInvitation saved = invitationRepo.save(invitation);
        return toInviteDto(saved);
    }

    @Override
    @Transactional
    public PlatformInvitePreviewDto resolvePlatformInvite(String token) {
        PlatformUserInvitation invitation = requireActiveInvitation(token);
        return toInvitePreviewDto(invitation);
    }

    @Override
    @Transactional
    public LoginResponse acceptPlatformInvite(AcceptPlatformInviteRequest request) {
        String normalizedToken = normalizeToken(request.getToken());
        String normalizedPassword = request.getPassword() == null ? "" : request.getPassword().trim();

        if (normalizedPassword.length() < 8) {
            throw new IllegalArgumentException("Use at least 8 characters for the password.");
        }

        PlatformUserInvitation invitation = requireActiveInvitation(normalizedToken);
        ensureIdentityAvailable(invitation.getUsername(), invitation.getEmail());

        AppUser user = new AppUser();
        user.setUsername(invitation.getUsername());
        user.setEmail(invitation.getEmail());
        user.setPassword(passwordEncoder.encode(normalizedPassword));
        user.setRole(Role.PLATFORM_ADMIN);
        user.setPlatformAccess(true);
        user.setEnabled(true);
        user.setCreatedBy(invitation.getCreatedBy());

        AppUser savedUser = appUserRepo.save(user);
        invitation.setAccepted(true);
        invitationRepo.save(invitation);

        String jwt = jwtService.generateToken(savedUser.getEmail());
        return new LoginResponse(jwt, toUserDto(savedUser));
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

    private PlatformUserListItemDto buildUserListItem(AppUser user) {
        int tenantCount = tenantMemberRepo.countByUser_Id(user.getId());
        PlatformUserListItemDto item = new PlatformUserListItemDto();
        item.setId(user.getId());
        item.setUsername(user.getUsername());
        item.setEmail(user.getEmail());
        item.setRole(user.getRole());
        item.setPlatformAccess(user.isPlatformAccess() || user.getRole() == Role.PLATFORM_ADMIN);
        item.setActive(user.isEnabled());
        item.setTenantCount(tenantCount);
        item.setCreatedAt(user.getCreatedAt());
        return item;
    }

    private PlatformUserInviteDto toInviteDto(PlatformUserInvitation invitation) {
        return PlatformUserInviteDto.builder()
                .username(invitation.getUsername())
                .email(invitation.getEmail())
                .token(invitation.getToken())
                .expiresAt(invitation.getExpiresAt())
                .build();
    }

    private PlatformInvitePreviewDto toInvitePreviewDto(PlatformUserInvitation invitation) {
        return PlatformInvitePreviewDto.builder()
                .username(invitation.getUsername())
                .email(invitation.getEmail())
                .expiresAt(invitation.getExpiresAt())
                .build();
    }

    private UserDto toUserDto(AppUser user) {
        String formattedUsername = StringUtils.capitalize(user.getUsername().toLowerCase());
        return new UserDto(
                user.getId(),
                formattedUsername,
                user.getEmail(),
                user.getRole().name(),
                user.getPhoneNumber(),
                !Boolean.FALSE.equals(user.getEmailVerified()),
                !StringUtils.hasText(user.getPhoneNumber()) || !Boolean.FALSE.equals(user.getPhoneVerified()),
                user.isPlatformAccess(),
                user.isEnabled()
        );
    }

    private PlatformUserInvitation requireActiveInvitation(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Invalid or expired platform invite.");
        }

        PlatformUserInvitation invitation = invitationRepo.findByTokenAndAcceptedFalse(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired platform invite."));

        if (invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitationRepo.delete(invitation);
            throw new IllegalArgumentException("This platform invite has expired. Ask a ROOTS admin for a fresh link.");
        }

        return invitation;
    }

    private void ensureIdentityAvailable(String username, String email) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (appUserRepo.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already taken.");
        }
        if (appUserRepo.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already registered.");
        }
    }

    private Optional<PlatformUserInvitation> findReusableInvitation(String username, String email) {
        Optional<PlatformUserInvitation> pendingByEmail = invitationRepo.findByEmailIgnoreCaseAndAcceptedFalse(email);
        Optional<PlatformUserInvitation> pendingByUsername = invitationRepo.findByUsernameIgnoreCaseAndAcceptedFalse(username);

        if (pendingByEmail.isPresent() && pendingByUsername.isPresent()
                && !Objects.equals(pendingByEmail.get().getId(), pendingByUsername.get().getId())) {
            throw new IllegalArgumentException("Email or username is already tied to another pending platform invite.");
        }

        return pendingByEmail.isPresent() ? pendingByEmail : pendingByUsername;
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

    private String normalizeUsername(String username) {
        String normalized = StringUtils.trimWhitespace(username);
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("Username is required.");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        String normalized = StringUtils.trimWhitespace(email);
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("Email is required.");
        }
        return normalized.toLowerCase();
    }

    private String normalizeToken(String token) {
        return token == null ? "" : token.trim();
    }

    private String randomToken(int length) {
        StringBuilder token = new StringBuilder(length);
        for (int index = 0; index < length; index += 1) {
            token.append(TOKEN_CHARS.charAt(TOKEN_RANDOM.nextInt(TOKEN_CHARS.length())));
        }
        return token.toString();
    }
}
