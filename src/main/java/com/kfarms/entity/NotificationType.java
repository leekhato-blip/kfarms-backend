package com.kfarms.entity;

public enum NotificationType {
    FEED,       // Feed-related alerts (low feed, new feed stock, etc.)
    FISH,       // Fish stock, harvest, or mortality issues
    SUPPLIES,   // Supply Records
    LIVESTOCK,  // General livestock (layers, ducks, turkeys, etc.)
    LAYER,      // Specific to egg production and layers
    FINANCE,    // Sales, revenue, and expense alerts
    INVENTORY,  // Inventory-level alerts (low stock, expiry, etc.)
    SYSTEM,     // Internal system or configuration warnings
    GENERAL     // Miscellaneous or info-only alerts
}
