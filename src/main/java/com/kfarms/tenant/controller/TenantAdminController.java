package com.kfarms.tenant.controller;

import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.AppUser;
import com.kfarms.repository.AppUserRepository;
import com.kfarms.tenant.entity.Invitation;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.entity.TenantAuditAction;
import com.kfarms.tenant.entity.TenantAuditLog;
import com.kfarms.tenant.entity.TenantMember;
import com.kfarms.tenant.entity.TenantPlan;
import com.kfarms.tenant.entity.TenantRole;
import com.kfarms.tenant.repository.InvitationRepository;
import com.kfarms.tenant.repository.TenantMemberRepository;
import com.kfarms.tenant.service.TenantAuditLogService;
import com.kfarms.tenant.service.TenantPermissionCatalog;
import com.kfarms.tenant.service.TenantPermissionService;
import com.kfarms.tenant.service.TenantPlanGuardService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
public class TenantAdminController {

    private final TenantMemberRepository memberRepo;
    private final AppUserRepository userRepo;
    private final InvitationRepository invitationRepo;
    private final TenantAuditLogService auditLogService;
    private final TenantPlanGuardService planGuardService;
    private final TenantPermissionService tenantPermissionService;

    public record ChangeRoleRequest(@NotNull TenantRole role) {}
    public record SetActiveRequest(@NotNull Boolean active) {}
    public record CreateInvitationRequest(@NotBlank @Email String email, @NotBlank String role) {}
    public record UpdatePermissionProfileRequest(String customRoleName, List<String> permissions) {}

    private AppUser requireCurrentUser(Authentication auth) {
        String principal = auth.getName();
        return userRepo.findByUsername(principal)
                .or(() -> userRepo.findByEmail(principal))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private Tenant requireCurrentTenant() {
        return planGuardService.requireCurrentTenant();
    }

    private String actorLabel(AppUser actor) {
        if (actor == null) return "SYSTEM";
        String email = actor.getEmail();
        if (email != null && !email.isBlank()) return email.trim();
        return actor.getUsername();
    }

    private String displayRole(TenantRole role) {
        if (role == null) return "STAFF";
        return role.name();
    }

    private TenantRole parseInvitationRole(String rawRole) {
        String normalized = rawRole == null ? "" : rawRole.trim().toUpperCase();
        if (normalized.isBlank() || "USER".equals(normalized) || "STAFF".equals(normalized)) {
            return TenantRole.STAFF;
        }
        if ("ADMIN".equals(normalized)) return TenantRole.ADMIN;
        if ("MANAGER".equals(normalized)) return TenantRole.MANAGER;
        throw new IllegalArgumentException("Invalid role. Allowed values: ADMIN, MANAGER, STAFF");
    }

    private String normalizeCustomRoleName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private Map<String, Object> memberRow(TenantMember member) {
        AppUser user = member.getUser();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("memberId", member.getId());
        row.put("userId", user.getId());
        row.put("username", user.getUsername());
        row.put("email", user.getEmail());
        row.put("role", displayRole(member.getRole()));
        row.put("roleLabel", tenantPermissionService.resolveRoleLabel(member));
        row.put("customRoleName", member.getCustomRoleName());
        row.put("permissions", tenantPermissionService.resolvePermissionsForResponse(member));
        row.put("active", member.getActive());
        row.put("createdAt", member.getCreatedAt());
        row.put("updatedAt", member.getUpdatedAt());
        row.put("createdBy", member.getCreatedBy());
        row.put("updatedBy", member.getUpdatedBy());
        return row;
    }

    private Map<String, Object> invitationRow(Invitation invitation) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("invitationId", invitation.getId());
        row.put("email", invitation.getEmail());
        row.put("role", displayRole(invitation.getRole()));
        row.put("token", invitation.getToken());
        row.put("accepted", Boolean.TRUE.equals(invitation.getAccepted()));
        row.put("expiresAt", invitation.getExpiresAt());
        row.put("createdAt", invitation.getCreatedAt());
        row.put("createdBy", invitation.getCreatedBy());
        return row;
    }

    private Map<String, Object> auditRow(TenantAuditLog log) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("auditId", log.getId());
        row.put("action", log.getAction() != null ? log.getAction().name() : "");
        row.put("subjectType", log.getSubjectType());
        row.put("subjectId", log.getSubjectId());
        row.put("actor", log.getCreatedBy());
        row.put("targetName", log.getTargetName());
        row.put("targetEmail", log.getTargetEmail());
        row.put("previousValue", log.getPreviousValue());
        row.put("nextValue", log.getNextValue());
        row.put("description", log.getDescription());
        row.put("createdAt", log.getCreatedAt());
        return row;
    }

    private String randomToken(int len) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder builder = new StringBuilder(len);
        for (int index = 0; index < len; index += 1) {
            builder.append(chars.charAt(random.nextInt(chars.length())));
        }
        return builder.toString();
    }

    /** List members of current tenant */
    @GetMapping("/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listMembers() {
        Tenant tenant = requireCurrentTenant();
        planGuardService.requirePlanAccess(
                tenant,
                TenantPlan.PRO,
                "User management is available on Pro and Enterprise plans."
        );
        tenantPermissionService.requireAnyPermission(
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication(),
                "You do not have permission to view team access.",
                TenantPermissionCatalog.USERS_VIEW,
                TenantPermissionCatalog.USERS_MANAGE
        );
        Long tenantId = tenant.getId();

        List<TenantMember> members = memberRepo.findAllByTenantIdWithUser(tenantId);
        List<Map<String, Object>> out = members.stream()
                .map(this::memberRow)
                .toList();

        return ResponseEntity.ok(new ApiResponse<>(true, "Members fetched", out));
    }

    @GetMapping("/invitations")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listInvitations(Authentication auth) {
        Tenant tenant = requireCurrentTenant();
        planGuardService.requirePlanAccess(
                tenant,
                TenantPlan.PRO,
                "User management is available on Pro and Enterprise plans."
        );
        tenantPermissionService.requireAnyPermission(
                auth,
                "You do not have permission to view invitations.",
                TenantPermissionCatalog.USERS_VIEW,
                TenantPermissionCatalog.USERS_MANAGE
        );
        Long tenantId = tenant.getId();
        List<Map<String, Object>> invitations = invitationRepo
                .findByTenant_IdAndAcceptedFalseOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(this::invitationRow)
                .toList();

        return ResponseEntity.ok(new ApiResponse<>(true, "Invitations fetched", invitations));
    }

    @PostMapping("/invitations")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createInvitation(
            @Valid @RequestBody CreateInvitationRequest request,
            Authentication auth
    ) {
        Tenant tenant = requireCurrentTenant();
        planGuardService.requirePlanAccess(
                tenant,
                TenantPlan.PRO,
                "User management is available on Pro and Enterprise plans."
        );
        tenantPermissionService.requirePermission(
                auth,
                TenantPermissionCatalog.USERS_MANAGE,
                "You do not have permission to invite teammates."
        );
        AppUser actor = requireCurrentUser(auth);
        String actorName = actorLabel(actor);
        String email = request.email().trim().toLowerCase();
        TenantRole role = parseInvitationRole(request.role());

        AppUser invitedUser = userRepo.findByEmail(email).orElse(null);
        if (invitedUser != null) {
            boolean alreadyMember = memberRepo.existsByTenant_IdAndUser_IdAndActiveTrue(tenant.getId(), invitedUser.getId());
            if (alreadyMember) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "User is already a member", null));
            }
        }

        if (invitationRepo.existsByTenant_IdAndEmailIgnoreCaseAndAcceptedFalse(tenant.getId(), email)) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Pending invite already exists", null));
        }

        planGuardService.ensureSeatCapacityForInvite(tenant);

        Invitation invitation = new Invitation();
        invitation.setTenant(tenant);
        invitation.setEmail(email);
        invitation.setRole(role);
        invitation.setToken(randomToken(48));
        invitation.setExpiresAt(java.time.LocalDateTime.now().plusDays(7));
        invitation.setAccepted(false);
        invitation.setCreatedBy(actorName);
        invitation.setUpdatedBy(actorName);
        invitationRepo.save(invitation);

        auditLogService.record(
                tenant,
                actorName,
                TenantAuditAction.INVITATION_CREATED,
                "INVITATION",
                invitation.getId(),
                email,
                email,
                null,
                displayRole(role),
                "Invitation created for " + email + " as " + displayRole(role) + "."
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Invitation created", invitationRow(invitation))
        );
    }

    @DeleteMapping("/invitations/{invitationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Object>> revokeInvitation(
            @PathVariable Long invitationId,
            Authentication auth
    ) {
        Tenant tenant = requireCurrentTenant();
        planGuardService.requirePlanAccess(
                tenant,
                TenantPlan.PRO,
                "User management is available on Pro and Enterprise plans."
        );
        tenantPermissionService.requirePermission(
                auth,
                TenantPermissionCatalog.USERS_MANAGE,
                "You do not have permission to revoke invitations."
        );
        AppUser actor = requireCurrentUser(auth);
        String actorName = actorLabel(actor);

        Invitation invitation = invitationRepo.findByIdAndTenant_IdAndAcceptedFalse(invitationId, tenant.getId())
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));

        auditLogService.record(
                tenant,
                actorName,
                TenantAuditAction.INVITATION_REVOKED,
                "INVITATION",
                invitation.getId(),
                invitation.getEmail(),
                invitation.getEmail(),
                displayRole(invitation.getRole()),
                null,
                "Invitation revoked for " + invitation.getEmail() + "."
        );

        invitationRepo.delete(invitation);
        return ResponseEntity.ok(new ApiResponse<>(true, "Invitation revoked", null));
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> listAuditLogs(
            Authentication auth,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Tenant tenant = requireCurrentTenant();
        planGuardService.requirePlanAccess(
                tenant,
                TenantPlan.ENTERPRISE,
                "Audit log access is available on the Enterprise plan."
        );
        tenantPermissionService.requirePermission(
                auth,
                TenantPermissionCatalog.AUDIT_VIEW,
                "You do not have permission to view audit activity."
        );
        Long tenantId = tenant.getId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Map<String, Object>> result = auditLogService
                .search(tenantId, search, action, pageable)
                .map(this::auditRow);

        return ResponseEntity.ok(new ApiResponse<>(true, "Audit logs fetched", result));
    }

    /** Change role (ADMIN-only). Cannot change OWNER via this endpoint (safer). */
    @PutMapping("/members/{memberId}/role")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changeRole(
            @PathVariable Long memberId,
            @Valid @RequestBody ChangeRoleRequest req,
            Authentication auth
    ) {
        Tenant tenant = requireCurrentTenant();
        planGuardService.requirePlanAccess(
                tenant,
                TenantPlan.PRO,
                "User management is available on Pro and Enterprise plans."
        );
        tenantPermissionService.requirePermission(
                auth,
                TenantPermissionCatalog.USERS_MANAGE,
                "You do not have permission to change member roles."
        );
        Long tenantId = tenant.getId();
        AppUser actor = requireCurrentUser(auth);

        TenantMember target = memberRepo.findByIdAndTenant_Id(memberId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        // Safety: don’t allow changing OWNER here (handle ownership transfer separately later)
        if (target.getRole() == TenantRole.OWNER) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Cannot change OWNER role here", null));
        }

        // Extra safety: don’t allow setting someone to OWNER here
        if (req.role() == TenantRole.OWNER) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Use ownership transfer for OWNER role", null));
        }

        TenantRole previousRole = target.getRole();
        target.setRole(req.role());
        target.setCustomRoleName(null);
        target.setPermissionOverrides(null);
        String actorName = actorLabel(actor);
        target.setUpdatedBy(actorName);
        memberRepo.save(target);

        auditLogService.record(
                requireCurrentTenant(),
                actorName,
                TenantAuditAction.MEMBER_ROLE_CHANGED,
                "MEMBER",
                target.getId(),
                target.getUser().getUsername(),
                target.getUser().getEmail(),
                displayRole(previousRole),
                displayRole(target.getRole()),
                "Role changed for " + target.getUser().getEmail() + "."
        );

        return ResponseEntity.ok(new ApiResponse<>(true, "Role updated", memberRow(target)));
    }

    @PutMapping("/members/{memberId}/permissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updatePermissionProfile(
            @PathVariable Long memberId,
            @RequestBody UpdatePermissionProfileRequest req,
            Authentication auth
    ) {
        Tenant tenant = requireCurrentTenant();
        planGuardService.requirePlanAccess(
                tenant,
                TenantPlan.ENTERPRISE,
                "Advanced role permissions are available on the Enterprise plan."
        );
        tenantPermissionService.requirePermission(
                auth,
                TenantPermissionCatalog.USERS_MANAGE,
                "You do not have permission to manage advanced access."
        );

        AppUser actor = requireCurrentUser(auth);
        String actorName = actorLabel(actor);

        TenantMember target = memberRepo.findByIdAndTenant_Id(memberId, tenant.getId())
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (target.getRole() == TenantRole.OWNER) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Cannot change OWNER access profile here", null));
        }

        List<String> requestedPermissions = req.permissions() == null
                ? List.of()
                : req.permissions();
        String previousPermissions = String.join(", ", tenantPermissionService.resolvePermissionsForResponse(target));
        String serializedPermissions = tenantPermissionService.serializePermissions(
                new java.util.LinkedHashSet<>(requestedPermissions),
                target.getRole()
        );

        target.setCustomRoleName(normalizeCustomRoleName(req.customRoleName()));
        target.setPermissionOverrides(serializedPermissions);
        target.setUpdatedBy(actorName);
        memberRepo.save(target);

        auditLogService.record(
                tenant,
                actorName,
                TenantAuditAction.MEMBER_ROLE_CHANGED,
                "MEMBER_PERMISSION_PROFILE",
                target.getId(),
                target.getUser().getUsername(),
                target.getUser().getEmail(),
                previousPermissions,
                String.join(", ", tenantPermissionService.resolvePermissionsForResponse(target)),
                "Advanced access profile updated for " + target.getUser().getEmail() + "."
        );

        return ResponseEntity.ok(new ApiResponse<>(true, "Advanced access updated", memberRow(target)));
    }

    /** Deactivate/activate member */
    @PatchMapping("/members/{memberId}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setActive(
            @PathVariable Long memberId,
            @Valid @RequestBody SetActiveRequest req,
            Authentication auth
    ) {
        Tenant tenant = requireCurrentTenant();
        planGuardService.requirePlanAccess(
                tenant,
                TenantPlan.PRO,
                "User management is available on Pro and Enterprise plans."
        );
        tenantPermissionService.requirePermission(
                auth,
                TenantPermissionCatalog.USERS_MANAGE,
                "You do not have permission to change member access."
        );
        Long tenantId = tenant.getId();
        AppUser actor = requireCurrentUser(auth);

        TenantMember target = memberRepo.findByIdAndTenant_Id(memberId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (target.getRole() == TenantRole.OWNER && Boolean.FALSE.equals(req.active())) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Cannot deactivate the OWNER", null));
        }

        if (Boolean.TRUE.equals(req.active()) && !Boolean.TRUE.equals(target.getActive())) {
            planGuardService.ensureSeatCapacityForActivation(tenant);
        }

        Boolean previousActive = target.getActive();
        target.setActive(req.active());
        String actorName = actorLabel(actor);
        target.setUpdatedBy(actorName);
        memberRepo.save(target);

        auditLogService.record(
                requireCurrentTenant(),
                actorName,
                Boolean.TRUE.equals(req.active())
                        ? TenantAuditAction.MEMBER_ACTIVATED
                        : TenantAuditAction.MEMBER_DEACTIVATED,
                "MEMBER",
                target.getId(),
                target.getUser().getUsername(),
                target.getUser().getEmail(),
                String.valueOf(previousActive),
                String.valueOf(target.getActive()),
                (Boolean.TRUE.equals(req.active()) ? "Member activated: " : "Member deactivated: ")
                        + target.getUser().getEmail()
                        + "."
        );

        return ResponseEntity.ok(new ApiResponse<>(true, "Member updated", memberRow(target)));
    }

    /** Remove member (cannot remove OWNER) */
    @DeleteMapping("/members/{memberId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Object>> removeMember(
            @PathVariable Long memberId,
            Authentication auth
    ) {
        Tenant tenant = requireCurrentTenant();
        planGuardService.requirePlanAccess(
                tenant,
                TenantPlan.PRO,
                "User management is available on Pro and Enterprise plans."
        );
        tenantPermissionService.requirePermission(
                auth,
                TenantPermissionCatalog.USERS_MANAGE,
                "You do not have permission to remove members."
        );
        AppUser actor = requireCurrentUser(auth);
        String actorName = actorLabel(actor);

        TenantMember target = memberRepo.findByIdAndTenant_Id(memberId, tenant.getId())
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (target.getRole() == TenantRole.OWNER) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Cannot remove the OWNER", null));
        }

        auditLogService.record(
                tenant,
                actorName,
                TenantAuditAction.MEMBER_REMOVED,
                "MEMBER",
                target.getId(),
                target.getUser().getUsername(),
                target.getUser().getEmail(),
                displayRole(target.getRole()),
                null,
                "Member removed: " + target.getUser().getEmail() + "."
        );

        memberRepo.delete(target);
        return ResponseEntity.ok(new ApiResponse<>(true, "Member removed", null));
    }
}
