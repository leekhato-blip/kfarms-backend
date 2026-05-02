package com.kfarms.service.impl;

import com.kfarms.dto.InventoryRequestDto;
import com.kfarms.dto.InventoryResponseDto;
import com.kfarms.entity.Inventory;
import com.kfarms.entity.InventoryCategory;
import com.kfarms.entity.NotificationType;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.InventoryMapper;
import com.kfarms.repository.InventoryRepository;
import com.kfarms.repository.NotificationRepository;
import com.kfarms.service.InventoryService;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.repository.TenantRepository;
import com.kfarms.service.NotificationService;
import com.kfarms.tenant.service.TenantContext;
import com.kfarms.tenant.service.TenantRecordAuditService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository repo;
    private final NotificationService notification;
    private final NotificationRepository notificationRepo;
    private final TenantRepository tenantRepository;
    private final TenantRecordAuditService tenantRecordAuditService;

    @Override
    public InventoryResponseDto create(InventoryRequestDto dto) {
        Long tenantId = requireTenantId();
        String canonicalName = normalizeName(dto.getItemName());
        InventoryCategory category = parseCategory(dto.getCategory());

        Optional<Inventory> existing = repo.findByItemNameAndCategoryAndTenantId(canonicalName, category, tenantId);
        if (existing.isPresent() && !Boolean.TRUE.equals(existing.get().getDeleted())) {
            throw new IllegalArgumentException("Inventory item already exists. Edit it or adjust stock instead.");
        }

        Inventory entity = existing.orElseGet(Inventory::new);
        boolean restoringDeletedItem = existing.isPresent() && Boolean.TRUE.equals(entity.getDeleted());

        applyRequest(entity, dto, canonicalName, category);
        entity.setTenant(resolveTenant(tenantId));
        if (restoringDeletedItem) {
            entity.setDeleted(false);
            entity.setDeletedAt(null);
        }
        Inventory saved = repo.save(entity);
        tenantRecordAuditService.created(
                tenantId,
                entity.getCreatedBy(),
                "INVENTORY",
                saved.getId(),
                inventoryTargetName(saved),
                inventorySummary(saved),
                "Created inventory record for " + inventoryTargetName(saved) + "."
        );
        return InventoryMapper.toResponseDto(saved);
    }

    @Override
    public Map<String, Object> getAll(
            int page,
            int size,
            String itemName,
            String category,
            String status,
            LocalDate lastUpdated,
            Boolean deleted
    ) {
        Long tenantId = requireTenantId();

        Sort sort = Boolean.TRUE.equals(deleted)
                ? Sort.by(Sort.Direction.DESC, "deletedAt").and(Sort.by(Sort.Direction.DESC, "id"))
                : Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Inventory> spec = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenant").get("id"), tenantId));
            predicates.add(cb.equal(root.get("deleted"), Boolean.TRUE.equals(deleted)));

            if (itemName != null && !itemName.isBlank()) {
                String needle = "%" + itemName.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("itemName")), needle),
                        cb.like(cb.lower(cb.coalesce(root.get("sku"), "")), needle),
                        cb.like(cb.lower(cb.coalesce(root.get("supplierName"), "")), needle),
                        cb.like(cb.lower(cb.coalesce(root.get("storageLocation"), "")), needle)
                ));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), parseCategory(category)));
            }
            if (status != null && !status.isBlank()) {
                String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
                switch (normalizedStatus) {
                    case "OUT" -> predicates.add(cb.lessThanOrEqualTo(root.get("quantity"), 0));
                    case "LOW" -> predicates.add(cb.and(
                            cb.greaterThan(root.get("quantity"), 0),
                            cb.lessThanOrEqualTo(root.get("quantity"), root.get("minThreshold"))
                    ));
                    case "HEALTHY" -> predicates.add(cb.greaterThan(root.get("quantity"), root.get("minThreshold")));
                    default -> {
                    }
                }
            }
            if (lastUpdated != null) {
                predicates.add(cb.equal(root.get("lastUpdated"), lastUpdated));
            }
            return cb.and(predicates.toArray(new Predicate[0]));

        };

        Page<Inventory> inventoryPage = repo.findAll(spec, pageable);

        List<InventoryResponseDto> items = inventoryPage.getContent().stream()
                .map(InventoryMapper::toResponseDto)
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("page", page);
        result.put("size", size);
        result.put("totalItems", inventoryPage.getTotalElements());
        result.put("totalPages", inventoryPage.getTotalPages());
        result.put("hasNext", inventoryPage.hasNext());
        result.put("hasPrevious", inventoryPage.hasPrevious());

        return result;
    }

    @Override
    public InventoryResponseDto getById(Long id) {
        Long tenantId = requireTenantId();
        Optional<Inventory> inventory = repo.findByIdAndTenant_Id(id, tenantId)
                .filter(i -> !Boolean.TRUE.equals(i.getDeleted()));

        return inventory.map(InventoryMapper::toResponseDto).orElse(null);
    }

    @Override
    public InventoryResponseDto update(Long id, InventoryRequestDto request, String updateBy) {
        Long tenantId = requireTenantId();

        Inventory entity = repo.findByIdAndTenant_Id(id, tenantId)
                .filter(i -> !Boolean.TRUE.equals(i.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "id", id));
        String previousSummary = inventorySummary(entity);

        String canonicalName = normalizeName(request.getItemName());
        InventoryCategory category = parseCategory(request.getCategory());

        repo.findByItemNameAndCategoryAndTenantId(canonicalName, category, tenantId)
                .filter(existing -> !Objects.equals(existing.getId(), id))
                .filter(existing -> !Boolean.TRUE.equals(existing.getDeleted()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Inventory item already exists. Edit it or adjust stock instead.");
                });

        applyRequest(entity, request, canonicalName, category);
        entity.setUpdatedBy(updateBy);
        entity.setUpdatedAt(LocalDateTime.now());

        repo.save(entity);
        tenantRecordAuditService.updated(
                tenantId,
                updateBy,
                "INVENTORY",
                entity.getId(),
                inventoryTargetName(entity),
                previousSummary,
                inventorySummary(entity),
                "Updated inventory record for " + inventoryTargetName(entity) + "."
        );
        return InventoryMapper.toResponseDto(entity);
    }

    @Override
    public void delete(Long id, String deletedBy) {
        Long tenantId = requireTenantId();
        Inventory entity = repo.findByIdAndTenant_Id(id, tenantId)
                .filter(i -> !Boolean.TRUE.equals(i.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "id", id));
        String previousSummary = inventorySummary(entity);

        if (Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Inventory with ID: " + id + " soft deleted successfully");
        }

        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setUpdatedBy(deletedBy);
        repo.save(entity);
        tenantRecordAuditService.deleted(
                tenantId,
                deletedBy,
                "INVENTORY",
                entity.getId(),
                inventoryTargetName(entity),
                previousSummary,
                "Deleted inventory record for " + inventoryTargetName(entity) + "."
        );
    }

    @Override
    public void permanentDelete(Long id, String deletedBy) {
        Long tenantId = requireTenantId();
        Inventory entity = repo.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "id", id));
        repo.delete(entity);
    }

    @Override
    public void restore(Long id) {
        Long tenantId = requireTenantId();
        Inventory entity = repo.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "id", id));

        if (!Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Inventory record with ID " + id + " has already been restored");
        }

        repo.findByItemNameAndCategoryAndTenantId(entity.getItemName(), entity.getCategory(), tenantId)
                .filter(existing -> !Objects.equals(existing.getId(), id))
                .filter(existing -> !Boolean.TRUE.equals(existing.getDeleted()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("A live inventory item with the same name and category already exists.");
                });

        entity.setDeleted(false);
        entity.setDeletedAt(null);
        repo.save(entity);
    }

    // ✅ SUMMARY should NOT create notifications
    @Override
    public Map<String, Object> getSummary() {
        Long tenantId = requireTenantId();

        List<Inventory> all = repo.findAll()
                .stream()
                .filter(i -> belongsToTenant(i, tenantId))
                .filter(i -> !Boolean.TRUE.equals(i.getDeleted()))
                .toList();

        Map<String, Object> summary = new HashMap<>();

        summary.put("totalInventoryItems", all.size());
        summary.put("totalQuantity", all.stream().mapToInt(Inventory::getQuantity).sum());
        summary.put("inventoryValue", all.stream()
                .map(this::resolveTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        Map<String, Integer> categoryTotals = new HashMap<>();
        for (Inventory inv : all) {
            categoryTotals.merge(inv.getCategory().name(), inv.getQuantity(), Integer::sum);
        }
        summary.put("quantityByCategory", categoryTotals);

        long lowStockCount = all.stream()
                .filter(this::isLowStock)
                .filter(i -> i.getQuantity() > 0)
                .count();
        long outOfStockCount = all.stream()
                .filter(i -> i.getQuantity() != null && i.getQuantity() <= 0)
                .count();

        summary.put("lowStockCount", lowStockCount);
        summary.put("outOfStockCount", outOfStockCount);
        summary.put("healthyCount", Math.max(all.size() - lowStockCount - outOfStockCount, 0));

        // ✅ low stock list only (no notification creation here)
        List<Map<String, Object>> lowStock = all.stream()
                .filter(this::isLowStock)
                .sorted(Comparator.comparingInt(i -> Optional.ofNullable(i.getQuantity()).orElse(0)))
                .map(this::toSummaryItem)
                .toList();

        summary.put("lowStockItems", lowStock);

        all.stream()
                .map(Inventory::getLastUpdated)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .ifPresent(last -> summary.put("lastUpdated", last));

        return summary;
    }

    @Override
    public List<Map<String, Object>> getLowFeedItems() {
        Long tenantId = requireTenantId();

        List<Inventory> lowFeeds = repo.findAll().stream()
                .filter(i -> belongsToTenant(i, tenantId))
                .filter(i -> !Boolean.TRUE.equals(i.getDeleted()))
                .filter(i -> i.getCategory() == InventoryCategory.FEED)
                .filter(i -> i.getQuantity() <= i.getMinThreshold())
                .toList();

        List<Map<String, Object>> watchlist = new ArrayList<>();
        for (Inventory feed : lowFeeds) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", feed.getId());
            map.put("name", feed.getItemName());
            map.put("remaining", feed.getQuantity());
            map.put("unit", feed.getUnit());
            watchlist.add(map);
        }

        return watchlist;
    }

    @Override
    public InventoryResponseDto adjustStockById(Long id, int quantityChange, String note, String updatedBy) {
        Long tenantId = requireTenantId();
        Inventory inventory = repo.findByIdAndTenant_Id(id, tenantId)
                .filter(i -> !Boolean.TRUE.equals(i.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "id", id));
        String previousSummary = inventorySummary(inventory);

        int currentQty = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        int newQuantity = currentQty + quantityChange;
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Insufficient stock for " + inventory.getItemName());
        }

        inventory.setQuantity(newQuantity);
        inventory.setLastUpdated(LocalDate.now());
        inventory.setUpdatedAt(LocalDateTime.now());
        inventory.setUpdatedBy(updatedBy);
        if (note != null && !note.isBlank()) {
            inventory.setNote(note.trim());
        }

        Inventory saved = repo.save(inventory);
        maybeCreateLowStockNotification(saved, tenantId);
        tenantRecordAuditService.updated(
                tenantId,
                updatedBy,
                "INVENTORY",
                saved.getId(),
                inventoryTargetName(saved),
                previousSummary,
                inventorySummary(saved),
                "Adjusted inventory stock for " + inventoryTargetName(saved) + "."
        );
        return InventoryMapper.toResponseDto(saved);
    }

    @Override
    public void adjustStock(String itemName, InventoryCategory category, int quantityChange, String unit, String note) {

        Long tenantId = TenantContext.getTenantId();

        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("itemName is required");
        }

        String canonicalName = itemName.trim();

        Inventory inventory = repo.findByItemNameAndCategoryAndTenantId(canonicalName, category, tenantId)
                .filter(i -> !Boolean.TRUE.equals(i.getDeleted()))
                .orElseGet(() -> {
                    Inventory newInventory = new Inventory();
                    newInventory.setItemName(canonicalName);
                    newInventory.setCategory(category);
                    newInventory.setTenant(resolveTenant(tenantId));
                    newInventory.setQuantity(0);
                    newInventory.setUnit(unit != null ? unit : "units");
                    int defaultThreshold = com.kfarms.catalog.InventoryCatalog.getDefaultThreshold(canonicalName);
                    newInventory.setMinThreshold(defaultThreshold);
                    newInventory.setNote(note);
                    newInventory.setCreatedAt(LocalDateTime.now());
                    return newInventory;
                });

        int currentQty = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        int newQuantity = currentQty + quantityChange;

        if (newQuantity < 0) {
            throw new IllegalArgumentException("Insufficient stock for " + canonicalName);
        }

        inventory.setQuantity(newQuantity);
        inventory.setLastUpdated(LocalDate.now());
        inventory.setUpdatedAt(LocalDateTime.now());
        if (note != null && !note.isBlank()) inventory.setNote(note);

        Inventory saved = repo.save(inventory);
        maybeCreateLowStockNotification(saved, tenantId);
    }

    private void applyRequest(Inventory entity, InventoryRequestDto dto, String canonicalName, InventoryCategory category) {
        entity.setItemName(canonicalName);
        entity.setCategory(category);
        entity.setSku(blankToNull(dto.getSku()));
        entity.setQuantity(dto.getQuantity());
        entity.setUnit(dto.getUnit().trim());
        entity.setUnitCost(dto.getUnitCost());
        entity.setMinThreshold(dto.getMinThreshold() != null ? dto.getMinThreshold() : 0);
        entity.setSupplierName(blankToNull(dto.getSupplierName()));
        entity.setStorageLocation(blankToNull(dto.getStorageLocation()));
        entity.setNote(blankToNull(dto.getNote()));
        entity.setLastUpdated(dto.getLastUpdated() != null ? dto.getLastUpdated() : LocalDate.now());
    }

    private void maybeCreateLowStockNotification(Inventory inventory, Long tenantId) {
        if (inventory.getQuantity() <= inventory.getMinThreshold()) {

            String title = "Low Stock Alert: " + inventory.getItemName();
            String msg = "Item '" + inventory.getItemName() + "' is running low. (" +
                    inventory.getQuantity() + " " + inventory.getUnit() + " left)";

            // ✅ Don't repeat same alert within 24 hours
            LocalDateTime since = LocalDateTime.now().minusHours(24);

            if (!notificationRepo.existsRecentGlobalDuplicate(
                    tenantId,
                    NotificationType.GENERAL,
                    title,
                    msg,
                    since
            )) {
                notification.createNotification(
                        tenantId,
                        "GENERAL",
                        title,
                        msg,
                        null
                );
            }
        }
    }

    private Map<String, Object> toSummaryItem(Inventory inventory) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", inventory.getId());
        item.put("itemName", inventory.getItemName());
        item.put("sku", inventory.getSku());
        item.put("category", inventory.getCategory() != null ? inventory.getCategory().name() : null);
        item.put("quantity", inventory.getQuantity());
        item.put("unit", inventory.getUnit());
        item.put("threshold", inventory.getMinThreshold());
        item.put("unitCost", inventory.getUnitCost());
        item.put("totalValue", resolveTotalValue(inventory));
        item.put("supplierName", inventory.getSupplierName());
        item.put("storageLocation", inventory.getStorageLocation());
        item.put("note", inventory.getNote());
        item.put("lastUpdated", inventory.getLastUpdated());
        item.put("status", resolveStatusKey(inventory));
        return item;
    }

    private boolean isLowStock(Inventory inventory) {
        if (inventory == null || inventory.getQuantity() == null) {
            return false;
        }
        return inventory.getQuantity() <= Optional.ofNullable(inventory.getMinThreshold()).orElse(0);
    }

    private String resolveStatusKey(Inventory inventory) {
        int quantity = Optional.ofNullable(inventory.getQuantity()).orElse(0);
        int threshold = Optional.ofNullable(inventory.getMinThreshold()).orElse(0);
        if (quantity <= 0) return "out";
        if (quantity <= threshold) return "low";
        return "healthy";
    }

    private BigDecimal resolveTotalValue(Inventory inventory) {
        if (inventory.getUnitCost() == null || inventory.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        return inventory.getUnitCost().multiply(BigDecimal.valueOf(inventory.getQuantity()));
    }

    private InventoryCategory parseCategory(String category) {
        return InventoryCategory.valueOf(category.trim().toUpperCase(Locale.ROOT));
    }

    private String normalizeName(String itemName) {
        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("Item name is required");
        }
        return itemName.trim();
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    private boolean belongsToTenant(Inventory inventory, Long tenantId) {
        return inventory != null
                && inventory.getTenant() != null
                && Objects.equals(inventory.getTenant().getId(), tenantId);
    }

    private String inventoryTargetName(Inventory inventory) {
        String itemName = inventory != null ? inventory.getItemName() : null;
        return itemName != null && !itemName.isBlank() ? itemName.trim() : "Inventory record";
    }

    private String inventorySummary(Inventory inventory) {
        if (inventory == null) {
            return "";
        }
        return String.format(
                "%s • Qty %s %s • Category %s • Unit cost %s",
                inventoryTargetName(inventory),
                Optional.ofNullable(inventory.getQuantity()).orElse(0),
                Optional.ofNullable(inventory.getUnit()).orElse("units"),
                inventory.getCategory() != null ? inventory.getCategory().name() : "UNKNOWN",
                inventory.getUnitCost() != null ? inventory.getUnitCost().toPlainString() : "0"
        );
    }
}
