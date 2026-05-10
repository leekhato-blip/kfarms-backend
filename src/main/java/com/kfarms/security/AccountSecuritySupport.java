package com.kfarms.security;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

public final class AccountSecuritySupport {

    public static final int MIN_PASSWORD_LENGTH = 6;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?\\d{7,15}$");

    private AccountSecuritySupport() {
    }

    public static String normalizeEmail(String value) {
        return safe(value).toLowerCase(Locale.ROOT);
    }

    public static String normalizePhoneNumber(String value) {
        String raw = safe(value);
        if (!StringUtils.hasText(raw)) {
            return "";
        }

        String leadingPlus = raw.startsWith("+") ? "+" : "";
        String digits = raw.replaceAll("[^\\d]", "");
        if (!StringUtils.hasText(digits)) {
            return "";
        }

        return leadingPlus + digits;
    }

    public static boolean isValidEmail(String value) {
        return EMAIL_PATTERN.matcher(normalizeEmail(value)).matches();
    }

    public static boolean isValidPhoneNumber(String value) {
        return PHONE_PATTERN.matcher(normalizePhoneNumber(value)).matches();
    }

    public static void validatePassword(String value, int minimumLength) {
        String password = value == null ? "" : value;

        if (password.length() < minimumLength) {
            throw new IllegalArgumentException("Use at least " + minimumLength + " characters for your password.");
        }

        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Use at least one letter and one number in your password.");
        }
    }

    public static String maskEmail(String value) {
        String email = normalizeEmail(value);
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }

        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    public static String maskPhoneNumber(String value) {
        String phone = normalizePhoneNumber(value);
        if (!StringUtils.hasText(phone)) {
            return "";
        }

        String digits = phone.startsWith("+") ? phone.substring(1) : phone;
        if (digits.length() <= 4) {
            return phone;
        }

        String visibleSuffix = digits.substring(digits.length() - 4);
        String maskedPrefix = "*".repeat(Math.max(digits.length() - 4, 0));
        return phone.startsWith("+") ? "+" + maskedPrefix + visibleSuffix : maskedPrefix + visibleSuffix;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
