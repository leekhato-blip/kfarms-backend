package com.kfarms.service.impl;

import com.kfarms.dto.FeedRequestDto;
import com.kfarms.dto.FeedResponseDto;
import com.kfarms.entity.Feed;
import com.kfarms.entity.FeedBatchType;
import com.kfarms.entity.Inventory;
import com.kfarms.entity.InventoryCategory;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.FeedMapper;
import com.kfarms.repository.FeedRepository;
import com.kfarms.repository.InventoryRepository;
import com.kfarms.service.FeedService;
import com.kfarms.service.InventoryService;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.repository.TenantRepository;
import com.kfarms.tenant.service.TenantContext;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private static final long DEFAULT_BATCH_ID = 0L;
    private static final List<String> FEED_CATEGORY_KEYS = List.of(
            "starter",
            "grower",
            "finisher",
            "layer",
            "broiler",
            "noiler",
            "duck",
            "fish",
            "turkey",
            "fowl",
            "other"
    );

    private final FeedRepository repo;
    private final InventoryRepository inventoryRepo;
    private final InventoryService inventoryService;
    private final TenantRepository tenantRepository;

    @Override
    @Transactional
    public FeedResponseDto create(FeedRequestDto request) {
        Long tenantId = requireTenantId();

        Feed entity = new Feed();
        entity.setTenant(resolveTenant(tenantId));
        applyRequest(entity, request);

        Feed saved = repo.save(entity);
        consumeInventoryIfTracked(saved, saved.getQuantityUsed(), "Feed logged");
        return FeedMapper.toResponseDto(saved);
    }

    @Override
    public Map<String, Object> getAll(int page, int size, String batchType, LocalDate date, Boolean deleted) {
        Long tenantId = requireTenantId();
        FeedBatchType batchTypeEnum = parseBatchType(batchType);

        Sort sort = Boolean.TRUE.equals(deleted)
                ? Sort.by(Sort.Direction.DESC, "deletedAt").and(Sort.by(Sort.Direction.DESC, "id"))
                : Sort.by(Sort.Direction.DESC, "id");

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Feed> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenant").get("id"), tenantId));

            if (Boolean.TRUE.equals(deleted)) {
                predicates.add(cb.isTrue(root.get("deleted")));
            } else {
                predicates.add(cb.or(
                        cb.isNull(root.get("deleted")),
                        cb.isFalse(root.get("deleted"))
                ));
            }

            if (batchTypeEnum != null) {
                predicates.add(cb.equal(root.get("batchType"), batchTypeEnum));
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

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", feedPage.getNumber());
        result.put("size", feedPage.getSize());
        result.put("totalItems", feedPage.getTotalElements());
        result.put("totalPages", feedPage.getTotalPages());
        result.put("hasNext", feedPage.hasNext());
        result.put("hasPrevious", feedPage.hasPrevious());
        return result;
    }

    @Override
    public FeedResponseDto getById(Long id) {
        Feed entity = getTenantFeed(id, false);
        return FeedMapper.toResponseDto(entity);
    }

    @Override
    @Transactional
    public FeedResponseDto update(Long id, FeedRequestDto request, String updatedBy) {
        Feed entity = getTenantFeed(id, false);
        FeedSnapshot previous = FeedSnapshot.from(entity);

        applyRequest(entity, request);
        entity.setUpdatedBy(updatedBy);

        reconcileInventory(previous, entity);
        Feed saved = repo.save(entity);
        return FeedMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id, String deletedBy) {
        Feed entity = getTenantFeed(id, true);
        if (Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Feed record with ID " + id + " has already been deleted");
        }

        refundInventoryIfTracked(entity, "Feed record deleted");
        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setUpdatedBy(deletedBy);
        repo.save(entity);
    }

    @Override
    @Transactional
    public void permanentDelete(Long id, String deletedBy) {
        Long tenantId = requireTenantId();
        Feed entity = getTenantFeed(id, true);
        if (!Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Feed record with ID " + id + " must be moved to trash before permanent delete");
        }

        int deletedCount = repo.hardDeleteByIdAndTenantId(entity.getId(), tenantId);
        if (deletedCount == 0) {
            throw new ResourceNotFoundException("Feed", "id", id);
        }
    }

    @Override
    @Transactional
    public void restore(Long id) {
        Long tenantId = requireTenantId();
        Feed entity = repo.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Feed", "id", id));

        if (!Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Feed with ID " + id + " is not deleted");
        }

        consumeInventoryIfTracked(entity, safeQuantity(entity.getQuantityUsed()), "Feed record restored");
        entity.setDeleted(false);
        entity.setDeletedAt(null);
        repo.save(entity);
    }

    @Override
    public Map<String, Object> getSummary() {
        Long tenantId = requireTenantId();

        List<Feed> allFeeds = repo.findAll().stream()
                .filter(feed -> belongsToTenant(feed, tenantId))
                .filter(feed -> !Boolean.TRUE.equals(feed.getDeleted()))
                .toList();

        List<Inventory> feedInventory = inventoryRepo.findAll().stream()
                .filter(item -> belongsToTenant(item, tenantId))
                .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
                .filter(item -> item.getCategory() == InventoryCategory.FEED)
                .toList();

        Map<String, Object> summary = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        int totalQuantityUsed = allFeeds.stream()
                .mapToInt(feed -> safeQuantity(feed.getQuantityUsed()))
                .sum();
        int usedThisMonth = allFeeds.stream()
                .filter(feed -> isSameMonth(feed.getDate(), today))
                .mapToInt(feed -> safeQuantity(feed.getQuantityUsed()))
                .sum();

        BigDecimal monthlySpend = allFeeds.stream()
                .filter(feed -> isSameMonth(feed.getDate(), today))
                .map(this::calculateTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Integer> usageByCategory = buildCategoryTotalsFromFeeds(allFeeds);
        Map<String, Integer> stockByCategory = buildCategoryTotalsFromInventory(feedInventory);
        if (stockByCategory.values().stream().mapToInt(Integer::intValue).sum() == 0) {
            stockByCategory = usageByCategory;
        }

        List<Map<String, Object>> topFeedsByUsage = allFeeds.stream()
                .collect(Collectors.groupingBy(
                        this::resolveFeedDisplayName,
                        LinkedHashMap::new,
                        Collectors.summingInt(feed -> safeQuantity(feed.getQuantityUsed()))
                ))
                .entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(6)
                .map(entry -> mapOf(
                        "label", entry.getKey(),
                        "value", entry.getValue()
                ))
                .toList();

        List<Map<String, Object>> recentFeedTransactions = allFeeds.stream()
                .sorted(Comparator
                        .comparing(FeedServiceImpl::effectiveDateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Feed::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(feed -> mapOf(
                        "id", feed.getId(),
                        "type", feed.getBatchType() != null ? feed.getBatchType().name() : "UNKNOWN",
                        "batchType", feed.getBatchType() != null ? feed.getBatchType().name() : "UNKNOWN",
                        "feedName", resolveFeedDisplayName(feed),
                        "itemName", resolveFeedDisplayName(feed),
                        "quantity", safeQuantity(feed.getQuantityUsed()),
                        "quantityUsed", safeQuantity(feed.getQuantityUsed()),
                        "unitCost", feed.getUnitCost(),
                        "date", feed.getDate() != null ? feed.getDate() : feed.getCreatedAt()
                ))
                .toList();

        Map<String, Integer> countByType = allFeeds.stream()
                .collect(Collectors.groupingBy(
                        feed -> feed.getBatchType() != null ? feed.getBatchType().name() : "UNKNOWN",
                        LinkedHashMap::new,
                        Collectors.summingInt(feed -> 1)
                ));

        LocalDate latestFeedDate = allFeeds.stream()
                .map(Feed::getDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

        if (latestFeedDate == null) {
            LocalDateTime latestCreatedAt = allFeeds.stream()
                    .map(Feed::getCreatedAt)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
            if (latestCreatedAt != null) {
                summary.put("lastFeedDate", latestCreatedAt);
            }
        } else {
            summary.put("lastFeedDate", latestFeedDate);
        }

        int totalStockOnHand = feedInventory.stream()
                .mapToInt(item -> safeQuantity(item.getQuantity()))
                .sum();

        int lowStockCount = (int) feedInventory.stream()
                .filter(item -> safeQuantity(item.getQuantity()) <= item.getMinThreshold())
                .count();

        String unit = feedInventory.stream()
                .map(Inventory::getUnit)
                .filter(this::hasText)
                .findFirst()
                .orElse("kg");

        summary.put("totalFeeds", allFeeds.size());
        summary.put("totalQuantityUsed", totalQuantityUsed);
        summary.put("usedThisMonth", usedThisMonth);
        summary.put("monthlySpend", monthlySpend);
        summary.put("avgUnitCost", averageUnitCost(allFeeds));
        summary.put("topFeedsByUsage", topFeedsByUsage);
        summary.put("stockByCategory", stockByCategory);
        summary.put("feedBreakdown", toBreakdown(usageByCategory));
        summary.put("recentFeedTransactions", recentFeedTransactions);
        summary.put("countByType", countByType);
        summary.put("unit", unit);
        summary.put("totalStockOnHand", totalStockOnHand);
        summary.put("lowStockCount", lowStockCount);
        summary.put("reorderCount", lowStockCount);

        return summary;
    }

    private void applyRequest(Feed entity, FeedRequestDto request) {
        FeedBatchType resolvedBatchType = resolveBatchType(request, entity);
        String explicitFeedName = sanitize(request.getFeedName());
        boolean inventoryTracked = hasText(explicitFeedName) || Boolean.TRUE.equals(entity.getInventoryTracked());

        entity.setBatchType(resolvedBatchType);
        entity.setBatchId(resolveBatchId(request, entity));
        entity.setFeedName(resolveFeedName(explicitFeedName, entity, resolvedBatchType));
        entity.setQuantityUsed(resolveQuantityUsed(request, entity));
        entity.setUnitCost(request.getUnitCost() != null ? request.getUnitCost() : entity.getUnitCost());
        entity.setNote(request.getNote());
        entity.setDate(request.getDate() != null ? request.getDate() : entity.getDate() != null ? entity.getDate() : LocalDate.now());
        entity.setInventoryTracked(inventoryTracked);
    }

    private FeedBatchType resolveBatchType(FeedRequestDto request, Feed entity) {
        String raw = sanitize(request.getBatchType());
        if (!hasText(raw)) {
            if (entity.getBatchType() != null) {
                return entity.getBatchType();
            }
            throw new IllegalArgumentException("Batch type is required");
        }

        try {
            return FeedBatchType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid batchType: '" + raw + "'. Allowed values: " + Arrays.toString(FeedBatchType.values())
            );
        }
    }

    private Long resolveBatchId(FeedRequestDto request, Feed entity) {
        if (request.getBatchId() != null) {
            return request.getBatchId();
        }
        if (entity.getBatchId() != null) {
            return entity.getBatchId();
        }
        return DEFAULT_BATCH_ID;
    }

    private Integer resolveQuantityUsed(FeedRequestDto request, Feed entity) {
        Integer quantityUsed = request.getQuantityUsed() != null ? request.getQuantityUsed() : entity.getQuantityUsed();
        if (quantityUsed == null || quantityUsed < 1) {
            throw new IllegalArgumentException("Quantity used is required");
        }
        return quantityUsed;
    }

    private String resolveFeedName(String explicitFeedName, Feed entity, FeedBatchType batchType) {
        if (hasText(explicitFeedName)) {
            return explicitFeedName;
        }
        if (hasText(entity.getFeedName())) {
            return entity.getFeedName().trim();
        }
        return inferFeedName(batchType);
    }

    private void reconcileInventory(FeedSnapshot previous, Feed current) {
        if (previous.tracked && Boolean.TRUE.equals(current.getInventoryTracked()) && Objects.equals(normalize(previous.name), normalize(current.getFeedName()))) {
            int delta = safeQuantity(current.getQuantityUsed()) - previous.quantity;
            if (delta != 0) {
                inventoryService.adjustStock(
                        current.getFeedName(),
                        InventoryCategory.FEED,
                        -delta,
                        "kg",
                        "Feed edit delta (id " + current.getId() + ")"
                );
            }
            return;
        }

        if (previous.tracked) {
            adjustInventory(previous.name, previous.quantity, "Feed edit refund (id " + current.getId() + ")");
        }

        if (Boolean.TRUE.equals(current.getInventoryTracked())) {
            consumeInventoryIfTracked(current, safeQuantity(current.getQuantityUsed()), "Feed edit apply");
        }
    }

    private void consumeInventoryIfTracked(Feed feed, int quantity, String reason) {
        if (!Boolean.TRUE.equals(feed.getInventoryTracked())) {
            return;
        }
        if (!hasText(feed.getFeedName()) || quantity <= 0) {
            return;
        }
        inventoryService.adjustStock(
                feed.getFeedName(),
                InventoryCategory.FEED,
                -quantity,
                "kg",
                reason + " (feed id " + feed.getId() + ")"
        );
    }

    private void refundInventoryIfTracked(Feed feed, String reason) {
        if (!Boolean.TRUE.equals(feed.getInventoryTracked())) {
            return;
        }
        adjustInventory(feed.getFeedName(), safeQuantity(feed.getQuantityUsed()), reason + " (feed id " + feed.getId() + ")");
    }

    private void adjustInventory(String feedName, int quantity, String reason) {
        if (!hasText(feedName) || quantity <= 0) {
            return;
        }
        inventoryService.adjustStock(
                feedName,
                InventoryCategory.FEED,
                quantity,
                "kg",
                reason
        );
    }

    private Feed getTenantFeed(Long id, boolean includeDeleted) {
        Long tenantId = requireTenantId();
        Feed entity = repo.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Feed", "id", id));

        if (!includeDeleted && Boolean.TRUE.equals(entity.getDeleted())) {
            throw new ResourceNotFoundException("Feed", "id", id);
        }
        return entity;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Missing tenant context");
        }
        return tenantId;
    }

    private Tenant resolveTenant(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));
    }

    private boolean belongsToTenant(Feed feed, Long tenantId) {
        return feed != null
                && feed.getTenant() != null
                && Objects.equals(feed.getTenant().getId(), tenantId);
    }

    private boolean belongsToTenant(Inventory inventory, Long tenantId) {
        return inventory != null
                && inventory.getTenant() != null
                && Objects.equals(inventory.getTenant().getId(), tenantId);
    }

    private FeedBatchType parseBatchType(String batchType) {
        if (!hasText(batchType)) {
            return null;
        }
        try {
            return FeedBatchType.valueOf(batchType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid batchType: '" + batchType + "'. Allowed values: " + Arrays.toString(FeedBatchType.values())
            );
        }
    }

    private Map<String, Integer> buildCategoryTotalsFromFeeds(List<Feed> feeds) {
        Map<String, Integer> totals = emptyCategoryTotals();
        for (Feed feed : feeds) {
            String key = categoryKeyForFeed(feed);
            totals.put(key, totals.getOrDefault(key, 0) + safeQuantity(feed.getQuantityUsed()));
        }
        return totals;
    }

    private Map<String, Integer> buildCategoryTotalsFromInventory(List<Inventory> inventoryItems) {
        Map<String, Integer> totals = emptyCategoryTotals();
        for (Inventory item : inventoryItems) {
            String key = inferFeedCategory(item.getItemName());
            totals.put(key, totals.getOrDefault(key, 0) + safeQuantity(item.getQuantity()));
        }
        return totals;
    }

    private Map<String, Integer> emptyCategoryTotals() {
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (String key : FEED_CATEGORY_KEYS) {
            totals.put(key, 0);
        }
        return totals;
    }

    private List<Map<String, Object>> toBreakdown(Map<String, Integer> totals) {
        return totals.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> mapOf(
                        "label", labelForCategory(entry.getKey()),
                        "value", entry.getValue()
                ))
                .toList();
    }

    private BigDecimal averageUnitCost(List<Feed> allFeeds) {
        List<BigDecimal> values = allFeeds.stream()
                .map(Feed::getUnitCost)
                .filter(Objects::nonNull)
                .toList();

        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotalCost(Feed feed) {
        if (feed.getUnitCost() == null) {
            return BigDecimal.ZERO;
        }
        return feed.getUnitCost().multiply(BigDecimal.valueOf(safeQuantity(feed.getQuantityUsed())));
    }

    private String resolveFeedDisplayName(Feed feed) {
        if (hasText(feed.getFeedName())) {
            return feed.getFeedName().trim();
        }
        return inferFeedName(feed.getBatchType());
    }

    private String inferFeedName(FeedBatchType batchType) {
        if (batchType == null) {
            return "Other Feed";
        }

        return switch (batchType) {
            case LAYER -> "Layer Feed";
            case BROILER -> "Broiler Feed";
            case NOILER -> "Noiler Feed";
            case DUCK -> "Duck Feed";
            case FISH -> "Fish Feed";
            case TURKEY -> "Turkey Feed";
            case FOWL -> "Fowl Feed";
            case OTHER -> "Other Feed";
        };
    }

    private String inferFeedCategory(String feedName) {
        if (!hasText(feedName)) {
            return "other";
        }

        String value = feedName.trim().toLowerCase(Locale.ROOT);
        if (value.contains("starter")) return "starter";
        if (value.contains("grower")) return "grower";
        if (value.contains("finisher")) return "finisher";
        if (value.contains("layer")) return "layer";
        if (value.contains("broiler")) return "broiler";
        if (value.contains("noiler")) return "noiler";
        if (value.contains("duck")) return "duck";
        if (value.contains("fish") || value.contains("catfish") || value.contains("tilapia")) return "fish";
        if (value.contains("turkey")) return "turkey";
        if (value.contains("fowl") || value.contains("poul")) return "fowl";
        return "other";
    }

    private String categoryKeyForFeed(Feed feed) {
        if (feed == null) {
            return "other";
        }
        if (feed.getBatchType() != null) {
            return switch (feed.getBatchType()) {
                case LAYER -> "layer";
                case BROILER -> "broiler";
                case NOILER -> "noiler";
                case DUCK -> "duck";
                case FISH -> "fish";
                case TURKEY -> "turkey";
                case FOWL -> "fowl";
                case OTHER -> "other";
            };
        }
        return inferFeedCategory(resolveFeedDisplayName(feed));
    }

    private String labelForCategory(String category) {
        if (!hasText(category)) {
            return "Other";
        }
        return switch (category) {
            case "starter" -> "Starter";
            case "grower" -> "Grower";
            case "finisher" -> "Finisher";
            case "layer" -> "Layer";
            case "broiler" -> "Broiler";
            case "noiler" -> "Noiler";
            case "duck" -> "Duck";
            case "fish" -> "Fish";
            case "turkey" -> "Turkey";
            case "fowl" -> "Fowl";
            default -> "Other";
        };
    }

    private boolean isSameMonth(LocalDate value, LocalDate reference) {
        return value != null
                && reference != null
                && value.getMonth() == reference.getMonth()
                && value.getYear() == reference.getYear();
    }

    private static LocalDateTime effectiveDateTime(Feed feed) {
        if (feed.getDate() != null) {
            return feed.getDate().atStartOfDay();
        }
        return feed.getCreatedAt();
    }

    private int safeQuantity(Integer value) {
        return value != null ? value : 0;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String sanitize(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    private static final class FeedSnapshot {
        private final String name;
        private final int quantity;
        private final boolean tracked;

        private FeedSnapshot(String name, int quantity, boolean tracked) {
            this.name = name;
            this.quantity = quantity;
            this.tracked = tracked;
        }

        private static FeedSnapshot from(Feed feed) {
            return new FeedSnapshot(
                    feed.getFeedName(),
                    feed.getQuantityUsed() != null ? feed.getQuantityUsed() : 0,
                    Boolean.TRUE.equals(feed.getInventoryTracked())
            );
        }
    }
}
