package com.kfarms.settings.dto;

public record UserPreferencesDto(
        String themePreference,
        String landingPage,
        Boolean emailNotifications,
        Boolean pushNotifications,
        Boolean weeklySummary,
        Boolean compactTables
) {
}
