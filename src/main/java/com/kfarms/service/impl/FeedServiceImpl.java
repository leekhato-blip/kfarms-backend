package com.kfarms.service.impl;

import com.kfarms.dto.FeedRequestDto;
import com.kfarms.dto.FeedResponseDto;
import com.kfarms.entity.Feed;
import com.kfarms.entity.FeedBatchType;
import com.kfarms.entity.InventoryCategory;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.FeedMapper;
import com.kfarms.repository.FeedRepository;
import com.kfarms.service.FeedService;
import com.kfarms.service.InventoryService;
import com.kfarms.service.NotificationService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {
    private final FeedRepository repo;
    private final InventoryService inventoryService;
    private final NotificationService notification;


    // CREATE - add new feed
    @Override
    public FeedResponseDto create(FeedRequestDto dto) {
        Feed entity = FeedMapper.toEntity(dto);
        Feed saved = repo.save(entity);

        // auto update inventory after create
        inventoryService.adjustStock(
                saved.getFeedName(),
                InventoryCategory.FEED,
                -saved.getQuantityUsed(),
                "kg",
                "Consumed by batch" + saved.getBatchId()
        );
        return FeedMapper.toResponseDto(saved);
    }

    // READ - get all with filtering & pagination
    @Override
    public Map<String, Object> getAll(int page, int size, String batchType, LocalDate date) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());


        // Convert batchType string to enum (case-insensitive)
        FeedBatchType batchTypeEnum = null;
        if (batchType != null && !batchType.isBlank()) {
            try {
                batchTypeEnum = FeedBatchType.valueOf(batchType.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "Invalid batchType: '" + batchType + "' . Allowed values: "
                        + Arrays.toString(FeedBatchType.values())
                );
            }
        }

        final FeedBatchType batchTypeEnumFinal = batchTypeEnum;

        Specification<Feed> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (batchTypeEnumFinal != null) {
                predicates.add(cb.equal(root.get("batchType"), batchTypeEnumFinal));
            }
            if (date != null) {
                predicates.add(cb.equal(root.get("date"), date));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Feed> feedPage = repo.findAll(spec, pageable);
        List<FeedResponseDto> items = feedPage.getContent().stream()
                .map(FeedMapper::toResponseDto)
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("page", feedPage.getNumber());
        result.put("size", feedPage.getSize());
        result.put("totalItems", feedPage.getTotalElements());
        result.put("totalPages", feedPage.getTotalPages());
        result.put("hasNext", feedPage.hasNext());
        result.put("hasPrevious", feedPage.hasPrevious());

        return result;
    }

    // READ - get by ID
    @Override
    public FeedResponseDto getById(Long id) {
        Feed entity = repo.findById(id)
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Feed", "id", id));
        return FeedMapper.toResponseDto(entity);
    }

    // UPDATE - update existing Feed by ID
    @Override
    public FeedResponseDto update(Long id, FeedRequestDto request, String updatedBy) {
        Feed entity = repo.findById(id)
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Feed", "id", id));

        // update fields from request
        entity.setBatchType(FeedBatchType.valueOf(request.getBatchType().toUpperCase()));
        entity.setFeedName(request.getFeedName());
        entity.setBatchId(request.getBatchId());
        entity.setNote(request.getNote());
        entity.setQuantityUsed(request.getQuantityUsed());
        entity.setUpdatedBy(updatedBy);

        repo.save(entity);
        return FeedMapper.toResponseDto(entity);
    }

    // DELETE - delete by ID
    @Override
    public void delete(Long id, String deletedBy) {
        Feed entity = repo.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Feed", "id", id));

        if (Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Feed record with ID " + id + " has already been deleted");
        }
        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setUpdatedBy(deletedBy);
        repo.save(entity);
    }

    // RESTORE
    @Override
    public void restore(Long id) {
        Feed entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feed", "id", id));

        if (!Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Feed with ID " + id + " has already been restored");
        }

        entity.setDeleted(false);
        entity.setDeletedAt(LocalDateTime.now());
        repo.save(entity);
    }

    // SUMMARY
    // SUMMARY
    @Override
    public Map<String, Object> getSummary() {
        List<Feed> all = repo.findAll()
                .stream()
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .toList();

        Map<String, Object> summary = new HashMap<>();

        // -----------------------------
        // Existing summary fields
        // -----------------------------

        summary.put("totalFeeds", all.size());

        int totalQuantityUsed = all.stream()
                .mapToInt(f -> f.getQuantityUsed() != null ? f.getQuantityUsed() : 0)
                .sum();
        summary.put("totalQuantityUsed", totalQuantityUsed);

        Map<String, Long> countByType = all.stream()
                .collect(Collectors.groupingBy(f -> f.getBatchType().name(), Collectors.counting()));
        summary.put("countByType", countByType);

        Map<String, Integer> quantityByType = all.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getBatchType().name(),
                        Collectors.summingInt(f -> f.getQuantityUsed() != null ? f.getQuantityUsed() : 0)
                ));

        int grandTotal = quantityByType.values().stream().mapToInt(Integer::intValue).sum();
        List<Map<String, Object>> breakdown = new ArrayList<>();

        if (grandTotal > 0) {
            quantityByType.forEach((type, qty) -> {
                double percentage = (qty * 100.0) / grandTotal;

                String label;
                switch (type) {
                    case "LAYERS": label = "Poultry"; break;
                    case "FISH": label = "Fish"; break;
                    case "DUCKS": label = "Ducks"; break;
                    default: label = "others";
                }

                Map<String, Object> entry = new HashMap<>();
                entry.put("label", label);
                entry.put("value", Math.round(percentage));
                breakdown.add(entry);
            });
        }
        summary.put("feedBreakdown", breakdown);

        int usedThisMonth = all.stream()
                .filter(f -> f.getDate() != null && f.getDate().getMonth().equals(LocalDate.now().getMonth())
                        && f.getDate().getYear() == LocalDate.now().getYear())
                .mapToInt(f -> f.getQuantityUsed() != null ? f.getQuantityUsed() : 0)
                .sum();
        summary.put("usedThisMonth", usedThisMonth);

        all.stream()
                .map(Feed::getCreatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .ifPresent(lastDate -> summary.put("lastFeedDate", lastDate));

        if (usedThisMonth > 100) {
            notification.createNotification(
                    "FEED",
                    "High Feed Usage",
                    "Feed usage for this month has exceeded 100kg",
                    null
            );
        }

        if (totalQuantityUsed < 100) {
            notification.createNotification(
                    "FEED",
                    "Low Feed Activity",
                    "Feed consumption appears unusually low this month",
                    null
            );
        }

        // -----------------------------
        // NEW: Top feeds by usage
        // -----------------------------
        List<Map<String, Object>> topFeedsByUsage = all.stream()
                .filter(f -> f.getFeedName() != null && !f.getFeedName().isBlank())
                .collect(Collectors.groupingBy(
                        Feed::getFeedName,
                        Collectors.summingInt(f -> f.getQuantityUsed() != null ? f.getQuantityUsed() : 0)
                ))
                .entrySet()
                .stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("label", e.getKey());
                    m.put("value", e.getValue());
                    return m;
                })
                .toList();
        summary.put("topFeedsByUsage", topFeedsByUsage);

        // -----------------------------
        // NEW: Stock by category (starter/grower/finisher/etc)
        // This is inferred from feedName keywords.
        // -----------------------------
        Map<String, Integer> stockByCategory = new LinkedHashMap<>();
        stockByCategory.put("starter", 0);
        stockByCategory.put("grower", 0);
        stockByCategory.put("finisher", 0);
        stockByCategory.put("layer", 0);
        stockByCategory.put("broiler", 0);
        stockByCategory.put("fish", 0);
        stockByCategory.put("other", 0);

        // NOTE: Without inventory-on-hand per item, we can only classify USED quantities from feeds table.
        // If inventory provides real on-hand by product, we’ll overwrite this below (when available).
        all.forEach(f -> {
            int qty = (f.getQuantityUsed() != null ? f.getQuantityUsed() : 0);
            String key = inferFeedCategory(f.getFeedName());
            stockByCategory.put(key, stockByCategory.getOrDefault(key, 0) + qty);
        });
        summary.put("stockByCategory", stockByCategory);

        // -----------------------------
        // NEW: Recent feed transactions (from feeds table)
        // type = batchType (since Feed currently represents consumption records)
        // unitCost = null unless inventory transactions provide it (below)
        // -----------------------------
        List<Map<String, Object>> recentFeedTransactions = all.stream()
                .sorted((a, b) -> {
                    LocalDateTime da = a.getCreatedAt();
                    LocalDateTime db = b.getCreatedAt();
                    if (da == null && db == null) return 0;
                    if (da == null) return 1;
                    if (db == null) return -1;
                    return db.compareTo(da);
                })
                .limit(10)
                .map(f -> {
                    Map<String, Object> tx = new HashMap<>();
                    tx.put("id", f.getId());
                    tx.put("type", f.getBatchType() != null ? f.getBatchType().name() : "UNKNOWN");
                    tx.put("quantity", f.getQuantityUsed() != null ? f.getQuantityUsed() : 0);
                    tx.put("unitCost", null); // will be enhanced if inventory transactions exist
                    tx.put("date", f.getDate() != null ? f.getDate() : null);
                    return tx;
                })
                .toList();
        summary.put("recentFeedTransactions", recentFeedTransactions);

        // -----------------------------
        // NEW: Inventory-driven summary (safe reflection calls)
        // If your InventoryService has these, they’ll be used:
        //  - Map getCategorySummary(InventoryCategory category)
        //  - List<Map> getRecentTransactions(InventoryCategory category, int limit)
        // -----------------------------
        Map<String, Object> invSummary = tryInvokeMap(inventoryService, "getCategorySummary",
                new Class[]{InventoryCategory.class},
                new Object[]{InventoryCategory.FEED}
        );

        // defaults
        summary.put("lowStockCount", 0);
        summary.put("reorderCount", 0);
        summary.put("totalStockOnHand", 0);
        summary.put("unit", "kg");
        summary.put("avgUnitCost", 0.0);
        summary.put("monthlySpend", 0.0);

        if (invSummary != null && !invSummary.isEmpty()) {
            summary.put("lowStockCount", asInt(invSummary.get("lowStockCount")));
            summary.put("reorderCount", asInt(invSummary.get("reorderCount")));
            summary.put("totalStockOnHand", asInt(invSummary.get("totalStockOnHand")));
            summary.put("unit", invSummary.get("unit") != null ? invSummary.get("unit") : "kg");
            summary.put("avgUnitCost", asDouble(invSummary.get("avgUnitCost")));

            // If inventory already gives monthlySpend, use it, else estimate from usedThisMonth * avgUnitCost
            Object ms = invSummary.get("monthlySpend");
            if (ms != null) {
                summary.put("monthlySpend", asDouble(ms));
            } else {
                double avgCost = asDouble(summary.get("avgUnitCost"));
                summary.put("monthlySpend", usedThisMonth * avgCost);
            }
        }

        // Inventory-based recent transactions (if available): overwrite/merge unitCost + better types
        List<Map<String, Object>> invTx = tryInvokeListOfMaps(inventoryService, "getRecentTransactions",
                new Class[]{InventoryCategory.class, int.class},
                new Object[]{InventoryCategory.FEED, 10}
        );

        if (invTx != null && !invTx.isEmpty()) {
            // Expecting each tx map: id, type, quantity, unitCost, date
            summary.put("recentFeedTransactions", invTx);

            // If we have unitCost + quantity for this month, compute monthlySpend more accurately
            double spend = invTx.stream()
                    .filter(m -> isInCurrentMonth(m.get("date")))
                    .mapToDouble(m -> asDouble(m.get("unitCost")) * asDouble(m.get("quantity")))
                    .sum();
            if (spend > 0) summary.put("monthlySpend", spend);

            // If avgUnitCost missing, compute weighted avg from invTx (for current month or all tx)
            double totalQty = invTx.stream().mapToDouble(m -> asDouble(m.get("quantity"))).sum();
            double totalCost = invTx.stream().mapToDouble(m -> asDouble(m.get("unitCost")) * asDouble(m.get("quantity"))).sum();
            if (totalQty > 0) summary.put("avgUnitCost", totalCost / totalQty);
        }

        return summary;
    }

    // -----------------------------
    // Helpers (add below getSummary)
    // -----------------------------

    private String inferFeedCategory(String feedName) {
        if (feedName == null) return "other";
        String s = feedName.trim().toLowerCase();

        if (s.contains("starter")) return "starter";
        if (s.contains("grower")) return "grower";
        if (s.contains("finisher")) return "finisher";
        if (s.contains("layer")) return "layer";
        if (s.contains("broiler")) return "broiler";
        if (s.contains("fish") || s.contains("catfish") || s.contains("tilapia")) return "fish";

        return "other";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> tryInvokeMap(Object target, String method, Class<?>[] paramTypes, Object[] args) {
        try {
            var m = target.getClass().getMethod(method, paramTypes);
            Object out = m.invoke(target, args);
            if (out instanceof Map) return (Map<String, Object>) out;
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> tryInvokeListOfMaps(Object target, String method, Class<?>[] paramTypes, Object[] args) {
        try {
            var m = target.getClass().getMethod(method, paramTypes);
            Object out = m.invoke(target, args);
            if (out instanceof List) return (List<Map<String, Object>>) out;
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private int asInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return 0; }
    }

    private double asDouble(Object v) {
        if (v == null) return 0.0;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0.0; }
    }

    private boolean isInCurrentMonth(Object dateObj) {
        if (dateObj == null) return false;

        // If it's already a LocalDate
        if (dateObj instanceof LocalDate d) {
            LocalDate now = LocalDate.now();
            return d.getMonth() == now.getMonth() && d.getYear() == now.getYear();
        }

        // If it's a LocalDateTime
        if (dateObj instanceof LocalDateTime dt) {
            LocalDate now = LocalDate.now();
            return dt.getMonth() == now.getMonth() && dt.getYear() == now.getYear();
        }

        // If it's a String (ISO expected)
        try {
            String s = String.valueOf(dateObj);
            if (s.length() >= 10) {
                LocalDate d = LocalDate.parse(s.substring(0, 10));
                LocalDate now = LocalDate.now();
                return d.getMonth() == now.getMonth() && d.getYear() == now.getYear();
            }
        } catch (Exception ignored) {}

        return false;
    }


}
