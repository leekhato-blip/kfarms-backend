package com.kfarms.security;

import com.kfarms.config.KfarmsCookieProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthCookieFactoryTest {

    @Test
    void createsConfiguredSessionCookie() {
        KfarmsCookieProperties properties = new KfarmsCookieProperties();
        properties.setSecure(true);
        properties.setSameSite("None");
        properties.setDomain(".example.com");
        properties.setMaxAgeDays(3);

        AuthCookieFactory factory = new AuthCookieFactory(properties);
        ResponseCookie cookie = factory.createSessionCookie("signed-token");

        assertEquals(JwtCookie.ACCESS_COOKIE, cookie.getName());
        assertEquals("signed-token", cookie.getValue());
        assertEquals(".example.com", cookie.getDomain());
        assertEquals("None", cookie.getSameSite());
        assertEquals(Duration.ofDays(3), cookie.getMaxAge());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
    }

    @Test
    void clearsCookieImmediately() {
        KfarmsCookieProperties properties = new KfarmsCookieProperties();
        AuthCookieFactory factory = new AuthCookieFactory(properties);

        ResponseCookie cookie = factory.clearSessionCookie();

        assertEquals("", cookie.getValue());
        assertEquals(Duration.ZERO, cookie.getMaxAge());
        assertTrue(cookie.isHttpOnly());
    }
}
