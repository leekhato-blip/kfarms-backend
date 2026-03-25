package com.kfarms.security;

import com.kfarms.config.KfarmsCookieProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AuthCookieFactory {

    private final KfarmsCookieProperties cookieProperties;

    public ResponseCookie createSessionCookie(String token) {
        long maxAgeDays = Math.max(1L, cookieProperties.getMaxAgeDays());
        return buildCookie(token, Duration.ofDays(maxAgeDays));
    }

    public ResponseCookie clearSessionCookie() {
        return buildCookie("", Duration.ZERO);
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(JwtCookie.ACCESS_COOKIE, value)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .path(JwtCookie.PATH)
                .maxAge(maxAge)
                .sameSite(cookieProperties.getSameSite());

        if (StringUtils.hasText(cookieProperties.getDomain())) {
            builder.domain(cookieProperties.getDomain().trim());
        }

        return builder.build();
    }
}
