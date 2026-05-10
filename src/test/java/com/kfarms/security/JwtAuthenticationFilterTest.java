package com.kfarms.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class JwtAuthenticationFilterTest {

    @Test
    void skipsPublicVerificationEndpoints() {
        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(mock(JwtService.class), mock(CustomUserDetailsService.class));

        assertTrue(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/api/auth/verify-contact")));
        assertTrue(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/api/auth/resend-contact-verification")));
        assertTrue(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/auth/verify-contact")));
        assertTrue(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/auth/resend-contact-verification")));
        assertFalse(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/api/auth/me")));
    }
}
