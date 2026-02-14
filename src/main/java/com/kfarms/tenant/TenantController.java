package com.kfarms.tenant;

import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.AppUser;
import com.kfarms.repository.AppUserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantRepository tenantRepo;
    private final TenantMemberRepository memberRepo;
    private final AppUserRepository userRepo;
    private final InvitationRepository invitationRepo;

    // --------- Requests ---------

    public record CreateTenantRequest(
            @NotBlank String name,
            @NotBlank String slug
    ) {}

    public record InviteRequest(
            @NotBlank String email,
            TenantRole role
    ) {}

    public record AcceptInviteRequest(
            @NotBlank String token
    ) {}

    // --------- Tenant Bootstrap ---------

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTenant(
            @Valid @RequestBody CreateTenantRequest req,
            Authentication auth
    ) {
        String principal = auth.getName();
        System.out.println("AUTH NAME: " + auth.getName());

        AppUser user = userRepo.findByUsername(principal)
                .or(() -> userRepo.findByEmail(principal))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Basic slug normalize (simple + safe)
        String slug = normalizeSlug(req.slug());

        // Prevent duplicates
        boolean slugExists = tenantRepo.existsBySlugIgnoreCase(slug);
        if (slugExists) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Slug already taken", null));
        }

        Tenant tenant = new Tenant();
        tenant.setName(req.name().trim());
        tenant.setSlug(slug);
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setPlan(PlanType.FREE);
        tenant.setCreatedBy(principal);

        tenantRepo.save(tenant);

        TenantMember member = new TenantMember();
        member.setTenant(tenant);
        member.setUser(user);
        member.setRole(TenantRole.OWNER);
        member.setActive(true);
        member.setCreatedBy(principal);

        memberRepo.save(member);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tenantId", tenant.getId());
        data.put("name", tenant.getName());
        data.put("slug", tenant.getSlug());
        data.put("plan", tenant.getPlan());
        data.put("status", tenant.getStatus());
        data.put("myRole", member.getRole());

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Tenant created", data)
        );
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> myTenants(Authentication auth) {
        String principal = auth.getName();
        System.out.println("AUTH NAME: " + auth.getName());

        AppUser user = userRepo.findByUsername(principal)
                .or(() -> userRepo.findByEmail(principal))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<TenantMember> memberships = memberRepo.findAllActiveWithTenant(user.getId());

        List<Map<String, Object>> out = memberships.stream()
                .map(m -> {
                    Tenant t = m.getTenant();
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("tenantId", t.getId());
                    row.put("name", t.getName());
                    row.put("slug", t.getSlug());
                    row.put("status", t.getStatus());
                    row.put("plan", t.getPlan());
                    row.put("myRole", m.getRole());
                    return row;
                })
                .toList();

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Tenants fetched", out)
        );
    }

    // --------- Invites ---------

    @PostMapping("/invites/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> acceptInvite(
            @Valid @RequestBody AcceptInviteRequest req,
            Authentication auth
    ) {
        String principal = auth.getName();
        System.out.println("AUTH NAME: " + auth.getName());

        AppUser user = userRepo.findByUsername(principal)
                .or(() -> userRepo.findByEmail(principal))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Invitation inv = invitationRepo.findByTokenAndAcceptedFalse(req.token().trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite token"));

        // 1) Expiry check (safe)
        if (inv.getExpiresAt() != null && inv.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Invite expired", null));
        }

        // 2) Email must match logged-in user
        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(inv.getEmail())) {
            return ResponseEntity.status(403)
                    .body(new ApiResponse<>(false, "Invite email does not match your account", null));
        }

        Long tenantId = inv.getTenant().getId();

        // Prevent duplicates
        boolean alreadyMember = memberRepo.existsByTenant_IdAndUser_IdAndActiveTrue(tenantId, user.getId());
        if (alreadyMember) {
            inv.setAccepted(true);
            inv.setUpdatedBy(principal);
            invitationRepo.save(inv);

            Map<String, Object> data = Map.of("tenantId", tenantId, "status", "already_member");
            return ResponseEntity.ok(new ApiResponse<>(true, "Already a member", data));
        }

        TenantMember member = new TenantMember();
        member.setTenant(inv.getTenant());
        member.setUser(user);
        member.setRole(inv.getRole());
        member.setActive(true);
        member.setCreatedBy(principal);
        memberRepo.save(member);

        inv.setAccepted(true);
        inv.setUpdatedBy(principal);
        invitationRepo.save(inv);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tenantId", tenantId);
        data.put("role", member.getRole());
        data.put("status", "joined");

        return ResponseEntity.ok(new ApiResponse<>(true, "Invite accepted", data));
    }

    @PostMapping("/{tenantId}/invites")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> invite(
            @PathVariable Long tenantId,
            @Valid @RequestBody InviteRequest req,
            Authentication auth
    ) {
        String principal = auth.getName();
        System.out.println("AUTH NAME: " + auth.getName());

        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        AppUser inviter = userRepo.findByUsername(principal)
                .or(() -> userRepo.findByEmail(principal))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Because /api/tenants/** is skipped by tenant filters, we enforce membership manually here
        TenantMember membership = memberRepo.findByTenant_IdAndUser_IdAndActiveTrue(tenantId, inviter.getId())
                .orElse(null);

        if (membership == null) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Not a member of this tenant", null));
        }

        if (!(membership.getRole() == TenantRole.OWNER || membership.getRole() == TenantRole.ADMIN)) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Only tenant admins can invite", null));
        }

        String email = req.email().trim().toLowerCase();
        TenantRole role = (req.role() == null) ? TenantRole.STAFF : req.role();

        // Guard: if invited user already exists and is already a member, block
        AppUser invitedUser = userRepo.findByEmail(email).orElse(null);
        if (invitedUser != null) {
            boolean alreadyMember = memberRepo.existsByTenant_IdAndUser_IdAndActiveTrue(tenantId, invitedUser.getId());
            if (alreadyMember) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "User is already a member", null));
            }
        }

        // Guard: pending invite already exists
        if (invitationRepo.existsByTenant_IdAndEmailIgnoreCaseAndAcceptedFalse(tenantId, email)) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Pending invite already exists", null));
        }

        Invitation inv = new Invitation();
        inv.setTenant(tenant);
        inv.setEmail(email);
        inv.setRole(role);
        inv.setToken(randomToken(48));
        inv.setExpiresAt(LocalDateTime.now().plusDays(7));
        inv.setAccepted(false);
        inv.setCreatedBy(principal);

        invitationRepo.save(inv);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tenantId", tenantId);
        data.put("email", email);
        data.put("role", role);
        data.put("token", inv.getToken()); // later you email this

        return ResponseEntity.ok(new ApiResponse<>(true, "Invitation created", data));
    }

    // --------- Helpers ---------

    private String normalizeSlug(String raw) {
        String s = (raw == null) ? "" : raw.trim().toLowerCase();
        s = s.replaceAll("[^a-z0-9\\-]+", "-");
        s = s.replaceAll("-{2,}", "-");
        s = s.replaceAll("(^-|-$)", "");
        if (s.isBlank()) throw new IllegalArgumentException("Slug is required");
        return s;
    }

    private String randomToken(int len) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }
}
