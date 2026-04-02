package com.kfarms.demo;

public final class DemoAccountSupport {

    public static final String DEMO_VIEWER_TENANT_NAME = "KFarms Demo Farm";
    public static final String DEMO_VIEWER_EMAIL = "demo.viewer@demo.kfarms.local";
    public static final String DEMO_VIEWER_USERNAME = "demo.viewer";
    public static final String DEMO_VIEWER_PASSWORD = "FarmDemo@2026";
    public static final String DEMO_VIEWER_TENANT_SLUG = "kfarms-demo";
    public static final String DEMO_VIEWER_TENANT_NOTE =
            "Read-only sample workspace with poultry, fish, feed, inventory, and sales data.";
    public static final String DEMO_VIEWER_BLOCKED_MESSAGE =
            "This is a demo account. Changes are disabled because the data is not real.";

    private DemoAccountSupport() {
    }

    public static boolean isDemoViewerEmail(String value) {
        return DEMO_VIEWER_EMAIL.equalsIgnoreCase(String.valueOf(value).trim());
    }
}
