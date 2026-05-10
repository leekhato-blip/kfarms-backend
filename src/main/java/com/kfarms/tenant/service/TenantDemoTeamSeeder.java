package com.kfarms.tenant.service;

import com.kfarms.entity.AppUser;
import com.kfarms.entity.Role;
import com.kfarms.repository.AppUserRepository;
import com.kfarms.tenant.entity.Invitation;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.entity.TenantAuditAction;
import com.kfarms.tenant.entity.TenantMember;
import com.kfarms.tenant.entity.TenantRole;
import com.kfarms.tenant.repository.InvitationRepository;
import com.kfarms.tenant.repository.TenantMemberRepository;
import com.kfarms.tenant.repository.TenantRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kfarms.demo.seed-team", havingValue = "true")
public class TenantDemoTeamSeeder implements ApplicationRunner {

    private static final String ACTOR = "SYSTEM:DEMO_TEAM_SEEDER";
    private static final String DEMO_PASSWORD = "DemoTeam@2026";
    private static final List<String> FIRST_NAMES = List.of(
            "Amina", "David", "Esther", "Samuel", "Fatima", "Chinedu",
            "Grace", "Moses", "Ifeoma", "Peter", "Ruth", "Daniel",
            "Aisha", "Emmanuel", "Mary", "Joshua", "Zainab", "Kelvin",
            "Naomi", "Ibrahim", "Mercy", "Tunde", "Lilian", "Victor"
    );
    private static final List<String> LAST_NAMES = List.of(
            "Bello", "Okoro", "Mensah", "Adeyemi", "Ibrahim", "Nwosu",
            "Kariuki", "Boateng", "Abubakar", "Omondi", "Afolayan", "Mutua",
            "Ajayi", "Okafor", "Adewale", "Yakubu", "Mwangi", "Sarpong",
            "Musa", "Adomako", "Ojo", "Eze", "Kamau", "Diallo"
    );

    private final TenantRepository tenantRepository;
    private final TenantMemberRepository tenantMemberRepository;
    private final InvitationRepository invitationRepository;
    private final AppUserRepository appUserRepository;
    private final TenantAuditLogService tenantAuditLogService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    private record DemoIdentity(String fullName, String email) {}

    private record DemoMemberSpec(String slotKey, TenantRole role, boolean active, int offset) {}

    private record DemoInviteSpec(String slotKey, TenantRole role, int offset, long expiresInHours) {}

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Tenant> tenants = tenantRepository.findAll();
        int seededTenants = 0;

        for (Tenant tenant : tenants) {
            if (Boolean.TRUE.equals(tenant.getDeleted())) {
                continue;
            }

            seedTenant(tenant);
            seededTenants += 1;
        }

        log.info("Demo tenant team seeder processed {} tenant(s).", seededTenants);
    }

    private void seedTenant(Tenant tenant) {
        String tenantKey = tenantKey(tenant);

        List<DemoMemberSpec> demoMembers = List.of(
                new DemoMemberSpec("admin", TenantRole.ADMIN, true, 0),
                new DemoMemberSpec("manager", TenantRole.MANAGER, true, 4),
                new DemoMemberSpec("staff", TenantRole.STAFF, true, 8),
                new DemoMemberSpec("inactive", TenantRole.STAFF, false, 12)
        );

        for (DemoMemberSpec spec : demoMembers) {
            DemoIdentity identity = buildIdentity(tenant, tenantKey, spec.slotKey(), spec.offset());
            AppUser user = ensureDemoUser(tenant, identity);
            ensureDemoMembership(tenant, user, spec.role(), spec.active());
        }

        List<DemoInviteSpec> demoInvites = List.of(
                new DemoInviteSpec("invite-manager", TenantRole.MANAGER, 16, 120),
                new DemoInviteSpec("invite-staff", TenantRole.STAFF, 20, 18)
        );

        for (DemoInviteSpec spec : demoInvites) {
            DemoIdentity identity = buildIdentity(tenant, tenantKey, spec.slotKey(), spec.offset());
            ensurePendingInvitation(
                    tenant,
                    identity.email(),
                    spec.role(),
                    LocalDateTime.now().plusHours(spec.expiresInHours())
            );
        }
    }

    private DemoIdentity buildIdentity(Tenant tenant, String tenantKey, String slotKey, int offset) {
        int tenantIndex = Math.toIntExact(tenant.getId());
        int firstIndex = Math.floorMod((tenantIndex * 5) + (offset * 3) + slotKey.length(), FIRST_NAMES.size());
        int lastIndex = Math.floorMod((tenantIndex * 7) + (offset * 5) + tenantKey.length(), LAST_NAMES.size());
        String fullName = FIRST_NAMES.get(firstIndex) + " " + LAST_NAMES.get(lastIndex);
        String email = emailLocalPart(fullName) + "+" + tenantKey + "@example.com";
        return new DemoIdentity(fullName, email);
    }

    private AppUser ensureDemoUser(Tenant tenant, DemoIdentity identity) {
        AppUser user = appUserRepository.findByEmail(identity.email()).orElse(null);
        if (user == null) {
            AppUser created = new AppUser();
            created.setUsername(resolveUsername(identity.fullName(), null, tenant));
            created.setEmail(identity.email());
            created.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
            created.setRole(Role.USER);
            created.setEnabled(true);
            created.setCreatedBy(ACTOR);
            created.setUpdatedBy(ACTOR);
            return appUserRepository.save(created);
        }

        boolean changed = false;
        String resolvedUsername = resolveUsername(identity.fullName(), user.getId(), tenant);
        if (!Objects.equals(user.getUsername(), resolvedUsername)) {
            user.setUsername(resolvedUsername);
            changed = true;
        }
        if (!user.isEnabled()) {
            user.setEnabled(true);
            changed = true;
        }
        if (user.getRole() != Role.USER) {
            user.setRole(Role.USER);
            changed = true;
        }

        if (changed) {
            user.setUpdatedBy(ACTOR);
            return appUserRepository.save(user);
        }

        return user;
    }

    private void ensureDemoMembership(Tenant tenant, AppUser user, TenantRole expectedRole, boolean expectedActive) {
        TenantMember member = tenantMemberRepository
                .findByTenant_IdAndUser_Id(tenant.getId(), user.getId())
                .orElseGet(() -> {
                    TenantMember created = new TenantMember();
                    created.setTenant(tenant);
                    created.setUser(user);
                    created.setRole(expectedRole);
                    created.setActive(expectedActive);
                    created.setCreatedBy(ACTOR);
                    created.setUpdatedBy(ACTOR);
                    TenantMember saved = tenantMemberRepository.save(created);

                    tenantAuditLogService.record(
                            tenant,
                            ACTOR,
                            TenantAuditAction.MEMBER_ROLE_CHANGED,
                            "MEMBER",
                            saved.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            "NONE",
                            displayRole(expectedRole),
                            "Demo member seeded as " + displayRole(expectedRole) + "."
                    );

                    if (!expectedActive) {
                        tenantAuditLogService.record(
                                tenant,
                                ACTOR,
                                TenantAuditAction.MEMBER_DEACTIVATED,
                                "MEMBER",
                                saved.getId(),
                                user.getUsername(),
                                user.getEmail(),
                                "true",
                                "false",
                                "Demo member access disabled for " + user.getEmail() + "."
                        );
                    }

                    return saved;
                });

        if (member.getRole() != expectedRole && member.getRole() != TenantRole.OWNER) {
            TenantRole previousRole = member.getRole();
            member.setRole(expectedRole);
            member.setUpdatedBy(ACTOR);
            tenantMemberRepository.save(member);

            tenantAuditLogService.record(
                    tenant,
                    ACTOR,
                    TenantAuditAction.MEMBER_ROLE_CHANGED,
                    "MEMBER",
                    member.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    displayRole(previousRole),
                    displayRole(expectedRole),
                    "Demo member role reset for " + user.getEmail() + "."
            );
        }

        if (member.isActive() != expectedActive && member.getRole() != TenantRole.OWNER) {
            boolean previousActive = member.isActive();
            member.setActive(expectedActive);
            member.setUpdatedBy(ACTOR);
            tenantMemberRepository.save(member);

            tenantAuditLogService.record(
                    tenant,
                    ACTOR,
                    expectedActive ? TenantAuditAction.MEMBER_ACTIVATED : TenantAuditAction.MEMBER_DEACTIVATED,
                    "MEMBER",
                    member.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    String.valueOf(previousActive),
                    String.valueOf(expectedActive),
                    expectedActive
                            ? "Demo member access restored for " + user.getEmail() + "."
                            : "Demo member access disabled for " + user.getEmail() + "."
            );
        }
    }

    private void ensurePendingInvitation(Tenant tenant, String email, TenantRole role, LocalDateTime expiresAt) {
        if (invitationRepository.existsByTenant_IdAndEmailIgnoreCaseAndAcceptedFalse(tenant.getId(), email)) {
            return;
        }

        Invitation invitation = new Invitation();
        invitation.setTenant(tenant);
        invitation.setEmail(email);
        invitation.setRole(role);
        invitation.setToken(randomToken(48));
        invitation.setExpiresAt(expiresAt);
        invitation.setAccepted(false);
        invitation.setCreatedBy(ACTOR);
        invitation.setUpdatedBy(ACTOR);
        invitationRepository.save(invitation);

        tenantAuditLogService.record(
                tenant,
                ACTOR,
                TenantAuditAction.INVITATION_CREATED,
                "INVITATION",
                invitation.getId(),
                email,
                email,
                null,
                displayRole(role),
                "Demo invitation created for " + email + " as " + displayRole(role) + "."
        );
    }

    private String resolveUsername(String preferred, Long currentUserId, Tenant tenant) {
        AppUser directMatch = appUserRepository.findByUsername(preferred).orElse(null);
        if (directMatch == null || Objects.equals(directMatch.getId(), currentUserId)) {
            return preferred;
        }

        String tenantScoped = preferred + " - " + shortTenantLabel(tenant);
        AppUser tenantMatch = appUserRepository.findByUsername(tenantScoped).orElse(null);
        if (tenantMatch == null || Objects.equals(tenantMatch.getId(), currentUserId)) {
            return tenantScoped;
        }

        return preferred + " - " + tenant.getId();
    }

    private String shortTenantLabel(Tenant tenant) {
        String raw = tenant.getName() == null || tenant.getName().isBlank()
                ? "Farm"
                : tenant.getName().trim();
        return raw.length() > 24 ? raw.substring(0, 24).trim() : raw;
    }

    private String tenantKey(Tenant tenant) {
        String raw = tenant.getSlug() == null || tenant.getSlug().isBlank()
                ? "tenant-" + tenant.getId()
                : tenant.getSlug();
        String normalized = raw.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "tenant-" + tenant.getId() : normalized;
    }

    private String emailLocalPart(String fullName) {
        String normalized = fullName.trim().toLowerCase().replaceAll("[^a-z0-9]+", ".");
        normalized = normalized.replaceAll("(^\\.+|\\.+$)", "");
        return normalized.isBlank() ? "team.member" : normalized;
    }

    private String displayRole(TenantRole role) {
        if (role == null) {
            return "STAFF";
        }
        return role.name();
    }

    private String randomToken(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index += 1) {
            builder.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return builder.toString();
    }
}
