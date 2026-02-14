package com.kfarms.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            if (HttpMethod.OPTIONS.matches(request.getMethod())) {
                filterChain.doFilter(request, response);
                return;
            }

            String path = request.getRequestURI();

            // Skip tenant header enforcement for auth + platform + tenant bootstrap
            if (path.startsWith("/auth/")
                    || path.startsWith("/api/auth")
                    || path.startsWith("/api/tenants")
                    || path.startsWith("/platform")) {
                filterChain.doFilter(request, response);
                return;
            }

            String tenantIdRaw = request.getHeader(TENANT_HEADER);
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

            TenantContext.setTenantId(tenantId);
            filterChain.doFilter(request, response);

        } finally {
            TenantContext.clear();
        }
    }
}
