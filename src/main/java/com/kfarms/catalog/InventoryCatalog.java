package com.kfarms.catalog;

import com.kfarms.entity.InventoryCategory;

import java.util.*;
import java.util.stream.Collectors;

public final class InventoryCatalog {

    private InventoryCatalog() {}

    // Canonical item names for FEED (livestock + fish only)
    private static final List<String> FISH_FEEDS = List.of(
            "Fish Feed 0.2mm",
            "Fish Feed 0.5mm",
            "Fish Feed 1mm",
            "Fish Feed 2mm",
            "Fish Feed 3mm",
            "Fish Feed 4mm",
            "Fish Feed 6mm",
            "Fish Feed 9mm",
            "Floating Fish Feed",
            "Sinking Fish Feed"
    );

    private static final List<String> LIVESTOCK_FEEDS = List.of(
            "Chicks Mash",
            "Growers Mash",
            "Pre-Layer Mash",
            "Layers Mash",
            "Noiler Starter",
            "Noiler Grower",
            "Noiler Finisher",
            "Turkey Starter",
            "Turkey Grower",
            "Turkey Finisher",
            "Duck Starter",
            "Duck Grower",
            "Duck Finisher"
    );

    // Default thresholds per canonical name
    private static final Map<String, Integer> DEFAULT_THRESHOLDS;
    static {
        Map<String,Integer> m = new HashMap<>();
        // Fish
        m.put("Fish Feed 0.2mm", 5);
        m.put("Fish Feed 0.5mm", 5);
        m.put("Fish Feed 1mm", 5);
        m.put("Fish Feed 2mm", 5);
        m.put("Fish Feed 3mm", 5);
        m.put("Fish Feed 4mm", 5);
        m.put("Fish Feed 6mm", 5);
        m.put("Fish Feed 9mm", 5);
        m.put("Floating Fish Feed", 5);
        m.put("Sinking Fish Feed", 5);
        // Livestock
        m.put("Chicks Mash", 5);
        m.put("Growers Mash", 5);
        m.put("Pre-Layer Mash", 5);
        m.put("Layers Mash", 5);
        m.put("Noiler Starter", 5);
        m.put("Noiler Grower", 5);
        m.put("Noiler Finisher", 5);
        m.put("Turkey Starter", 5);
        m.put("Turkey Grower", 5);
        m.put("Turkey Finisher", 5);
        m.put("Duck Starter", 5);
        m.put("Duck Grower", 5);
        m.put("Duck Finisher", 5);

        DEFAULT_THRESHOLDS = Collections.unmodifiableMap(m);
    }

    private static final Map<InventoryCategory, List<String>> ITEMS_BY_CATEGORY;
    static {
        Map<InventoryCategory, List<String>> map = new EnumMap<>(InventoryCategory.class);
        map.put(InventoryCategory.FISH, Collections.unmodifiableList(FISH_FEEDS));
        map.put(InventoryCategory.LAYER, Collections.unmodifiableList(LIVESTOCK_FEEDS.stream().filter(s -> s.toLowerCase().contains("layer")).collect(Collectors.toList())));
        map.put(InventoryCategory.NOILER, Collections.unmodifiableList(LIVESTOCK_FEEDS.stream().filter(s -> s.toLowerCase().contains("noiler")).collect(Collectors.toList())));
        map.put(InventoryCategory.FEED, Collections.unmodifiableList(LIVESTOCK_FEEDS)); // fallback: show livestock feed names when category=FEED
        // Also map turkey/duck if you have categories for them; otherwise they'll use FEED
        ITEMS_BY_CATEGORY = Collections.unmodifiableMap(map);
    }

    // Normalization helper: trim, collapse whitespace, lowercase
    private static String normalize(String s) {
        if (s == null) return null;
        return s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    // return canonical name if it matches known item (case-insensitive, tolerant)
    public static Optional<String> getCanonicalName(InventoryCategory category, String candidate) {
        if (candidate == null) return Optional.empty();
        String candNorm = normalize(candidate);

        // first check category-specific list
        List<String> byCat = ITEMS_BY_CATEGORY.getOrDefault(category, Collections.emptyList());
        for (String canonical : byCat) {
            if (normalize(canonical).equals(candNorm)) return Optional.of(canonical);
            // allow matches like "fish feed 1 mm" vs "fish feed 1mm" by removing non-alphanum
            if (normalize(canonical).replaceAll("[^a-z0-9]", "").equals(candNorm.replaceAll("[^a-z0-9]", "")))
                return Optional.of(canonical);
        }

        // fallback: check all known lists
        for (String canonical : FISH_FEEDS) {
            if (normalize(canonical).replaceAll("[^a-z0-9]", "").equals(candNorm.replaceAll("[^a-z0-9]", "")))
                return Optional.of(canonical);
        }
        for (String canonical : LIVESTOCK_FEEDS) {
            if (normalize(canonical).replaceAll("[^a-z0-9]", "").equals(candNorm.replaceAll("[^a-z0-9]", "")))
                return Optional.of(canonical);
        }

        return Optional.empty();
    }

    public static boolean isValidForCategory(InventoryCategory category, String candidate) {
        return getCanonicalName(category, candidate).isPresent();
    }

    public static List<String> itemsForCategory(InventoryCategory category) {
        // return copy to avoid accidental modification
        return new ArrayList<>(ITEMS_BY_CATEGORY.getOrDefault(category, Collections.emptyList()));
    }

    public static int getDefaultThreshold(String canonicalName) {
        return DEFAULT_THRESHOLDS.getOrDefault(canonicalName, 0);
    }
}
