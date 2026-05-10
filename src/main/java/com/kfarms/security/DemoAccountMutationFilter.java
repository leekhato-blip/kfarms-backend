package com.kfarms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfarms.demo.DemoAccountSupport;
import com.kfarms.entity.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class DemoAccountMutationFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_MUTATION_PATHS = Set.of(
            "/auth/logout",
            "/api/auth/logout"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        return HttpMethod.OPTIONS.matches(method)
                || HttpMethod.GET.matches(method)
                || HttpMethod.HEAD.matches(method)
                || ALLOWED_MUTATION_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!DemoAccountSupport.isDemoViewerEmail(authentication.getName())) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        objectMapper.writeValue(
                response.getWriter(),
                new ApiResponse<>(false, DemoAccountSupport.DEMO_VIEWER_BLOCKED_MESSAGE, null)
        );
    }
}
