package com.kfarms.config;

import com.kfarms.security.CustomUserDetailsService;
import com.kfarms.security.DemoAccountMutationFilter;
import com.kfarms.security.JwtAuthenticationFilter;
import com.kfarms.security.JwtService;
import com.kfarms.tenant.service.TenantFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.kfarms.tenant.service.TenantMembershipFilter;
import lombok.RequiredArgsConstructor;


import java.util.List;

@Configuration
@EnableWebSecurity // Enables Spring Security for the Farm app
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final KfarmsCorsProperties corsProperties;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService,
                                                           CustomUserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthFilter,
                                           DemoAccountMutationFilter demoAccountMutationFilter,
                                           TenantFilter tenantFilter,
                                           TenantMembershipFilter tenantMembershipFilter) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable()) // Disable CSRF useful for APIs
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                .requestMatchers("/auth/**", "/api/auth/**").permitAll()
                                .requestMatchers("/error", "/error/**").permitAll()
                                .requestMatchers("/api/billing/paystack/webhook").permitAll()
                                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()

                                // tenant bootstrap endpoints (no tenant header required)
                                .requestMatchers("/api/tenants/**").authenticated()

                                // ROOTS platform endpoints (global admin only)
                                .requestMatchers("/api/platform/**").hasRole("PLATFORM_ADMIN")

                                // everything else requires login (tenant filters + membership filter will enforce roles)
                                .anyRequest().authenticated()
                )

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(demoAccountMutationFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(tenantFilter, DemoAccountMutationFilter.class)
                .addFilterAfter(tenantMembershipFilter, TenantFilter.class);

        // Enable basic auth (username & password via browser/postman)
        return http.build(); // Build the security config

    }

    // This bean tells spring to use BCrypt to hash passwords securely
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder(); // Very secure hashing
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(sanitize(corsProperties.getAllowedOrigins()));
        config.setAllowedOriginPatterns(sanitize(corsProperties.getAllowedOriginPatterns()));
        config.setAllowedMethods(sanitize(corsProperties.getAllowedMethods()));
        config.setAllowedHeaders(sanitize(corsProperties.getAllowedHeaders()));
        config.setExposedHeaders(sanitize(corsProperties.getExposedHeaders()));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    private List<String> sanitize(List<String> values) {
        return values == null
                ? List.of()
                : values.stream()
                .map(value -> value == null ? "" : value.trim())
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }
}
