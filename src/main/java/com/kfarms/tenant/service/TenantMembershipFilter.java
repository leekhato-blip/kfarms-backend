package com.kfarms.tenant.service;

import com.kfarms.entity.AppUser;
import com.kfarms.repository.AppUserRepository;
import com.kfarms.tenant.repository.TenantRepository;
import com.kfarms.tenant.entity.TenantStatus;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.entity.TenantMember;
import com.kfarms.tenant.repository.TenantMemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TenantMembershipFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepo;
    private final TenantMemberRepository memberRepo;
    private final AppUserRepository appUserRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        if (uri.startsWith("/platform")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Allow preflight
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // Skip tenant membership enforcement for auth + tenant bootstrap + platform
        if (path.startsWith("/auth/")
                || path.startsWith("/api/auth")
                || path.equals("/api/billing/paystack/webhook")
                || path.startsWith("/api/tenants")     // list/create tenants, accept invites, etc.
                || path.startsWith("/platform")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Tenant MUST already be resolved by TenantFilter
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Missing tenant context\"}");
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            // Let Spring Security handle unauthenticated requests
            filterChain.doFilter(request, response);
            return;
        }

        // 1) Tenant exists + ACTIVE
        Tenant tenant = tenantRepo.findById(tenantId).orElse(null);
        if (tenant == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Invalid tenant\"}");
            return;
        }
        if (tenant.getStatus() == TenantStatus.SUSPENDED) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Tenant is suspended\"}");
            return;
        }

        // 2) Load current user
        String identity = auth.getName();
        AppUser user = appUserRepo.findByEmail(identity)
                .orElseGet(() -> appUserRepo.findByUsername(identity).orElse(null));

        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"User not found\"}");
            return;
        }

        // 3) Membership check
        TenantMember member = memberRepo
                .findByTenant_IdAndUser_IdAndActiveTrue(tenantId, user.getId())
                .orElse(null);

        if (member == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Not a member of this tenant\"}");
            return;
        }

        // 4) Apply tenant role as authorities so your @PreAuthorize keeps working
        List<SimpleGrantedAuthority> authorities = switch (member.getRole()) {
            case OWNER, ADMIN -> List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_MANAGER"),
                    new SimpleGrantedAuthority("ROLE_STAFF")
            );
            case MANAGER -> List.of(
                    new SimpleGrantedAuthority("ROLE_MANAGER"),
                    new SimpleGrantedAuthority("ROLE_STAFF")
            );
            case STAFF -> List.of(new SimpleGrantedAuthority("ROLE_STAFF"));
        };

        UsernamePasswordAuthenticationToken newAuth =
                new UsernamePasswordAuthenticationToken(auth.getPrincipal(), auth.getCredentials(), authorities);

        SecurityContextHolder.getContext().setAuthentication(newAuth);

        filterChain.doFilter(request, response);
    }
}
