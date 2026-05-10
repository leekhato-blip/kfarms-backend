package com.kfarms.security;

public final class JwtCookie {
    private JwtCookie() {}

    public static final String ACCESS_COOKIE = "kfarms_access"; // cookie name
    public static final String PATH = "/";

    // For local dev over http://localhost, Secure must be false.
    // For production HTTPS, Secure must be true.
}
