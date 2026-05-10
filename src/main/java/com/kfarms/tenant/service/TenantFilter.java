package com.kfarms.tenant.service;

import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.entity.TenantStatus;
import com.kfarms.tenant.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String TENANT_PARAM = "tenantId";

    private final TenantRepository tenantRepo;

    private boolean isPublicActuatorPath(String path) {
        return "/actuator".equals(path)
                || "/actuator/info".equals(path)
                || path.startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        boolean tenantContextBound = false;
        String uri = request.getRequestURI();

        if (uri.startsWith("/platform")
                || uri.startsWith("/api/platform")
                || uri.startsWith("/error")
                || isPublicActuatorPath(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (HttpMethod.OPTIONS.matches(request.getMethod())) {
                filterChain.doFilter(request, response);
                return;
            }

            String path = request.getRequestURI();

            // Skip tenant header enforcement for auth + platform + tenant bootstrap
            if (path.startsWith("/auth/")
                    || path.startsWith("/api/auth")
                    || path.startsWith("/api/platform")
                    || path.equals("/api/billing/paystack/webhook")
                    || path.startsWith("/api/tenants")
                    || path.startsWith("/platform")
                    || path.startsWith("/error")
                    || isPublicActuatorPath(path)) {
                filterChain.doFilter(request, response);
                return;
            }

            String tenantIdRaw = request.getHeader(TENANT_HEADER);
            if (tenantIdRaw == null || tenantIdRaw.isBlank()) {
                tenantIdRaw = request.getParameter(TENANT_PARAM);
            }
            if (tenantIdRaw == null || tenantIdRaw.isBlank()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Missing X-Tenant-Id header\"}");
                return;
            }

            Long tenantId;
            try {
                tenantId = Long.parseLong(tenantIdRaw);
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Invalid X-Tenant-Id header\"}");
                return;
            }

            Tenant tenant = tenantRepo.findById(tenantId)
                    .orElse(null);

            if (tenant == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Tenant not found\"}");
                return;
            }

            if (tenant.getStatus() == TenantStatus.SUSPENDED) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Tenant is suspended\"}");
                return;
            }

            TenantContext.setTenantId(tenantId);
            tenantContextBound = true;

            filterChain.doFilter(request, response);

        } finally {
            if (tenantContextBound) {
                TenantContext.clear();
            }
        }
    }
}
