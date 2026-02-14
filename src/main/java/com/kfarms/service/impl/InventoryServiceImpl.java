package com.kfarms.service.impl;

import com.kfarms.dto.InventoryRequestDto;
import com.kfarms.dto.InventoryResponseDto;
import com.kfarms.entity.Inventory;
import com.kfarms.entity.InventoryCategory;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.InventoryMapper;
import com.kfarms.repository.InventoryRepository;
import com.kfarms.repository.NotificationRepository;
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

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository repo;
    private final NotificationService notification;
    private final NotificationRepository notificationRepo;

    @Override
    public InventoryResponseDto create(InventoryRequestDto dto) {
        Inventory entity = InventoryMapper.toEntity(dto);
        Inventory saved = repo.save(entity);
        return InventoryMapper.toResponseDto(saved);
    }

    @Override
    public Map<String, Object> getAll(int page, int size, String itemName, String category, LocalDate lastUpdated) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Specification<Inventory> spec = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (itemName != null && !itemName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("itemName")), "%" + itemName.toLowerCase() + "%"));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("category")), "%" + category.toLowerCase() + "%"));
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
        Optional<Inventory> inventory = repo.findById(id)
                .filter(i -> !Boolean.TRUE.equals(i.getDeleted()));

        return inventory.map(InventoryMapper::toResponseDto).orElse(null);
    }

    @Override
    public InventoryResponseDto update(Long id, InventoryRequestDto request, String updateBy) {

        Inventory entity = repo.findById(id)
                .filter(i -> !Boolean.TRUE.equals(i.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "id", id));

        entity.setItemName(request.getItemName());
        entity.setCategory(InventoryCategory.valueOf(request.getCategory().toUpperCase()));
        entity.setQuantity(request.getQuantity());
        entity.setUnit(request.getUnit());
        entity.setLastUpdated(request.getLastUpdated());
        entity.setNote(request.getNote());
        entity.setUpdatedBy(updateBy);

        repo.save(entity);
        return InventoryMapper.toResponseDto(entity);
    }

    @Override
    public void delete(Long id, String deletedBy) {
        Inventory entity = repo.findById(id)
                .filter(i -> !Boolean.TRUE.equals(i.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "id", id));

        if (Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Inventory with ID: " + id + " soft deleted successfully");
        }

        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setUpdatedBy(deletedBy);
        repo.save(entity);
    }

    @Override
    public void restore(Long id) {
        Inventory entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "id", id));

        if (!Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Inventory record with ID " + id + " has already been restored");
        }

        entity.setDeleted(false);
        entity.setDeletedAt(null);
        repo.save(entity);
    }

    // ✅ SUMMARY should NOT create notifications
    @Override
    public Map<String, Object> getSummary() {

        List<Inventory> all = repo.findAll()
                .stream()
                .filter(i -> !Boolean.TRUE.equals(i.getDeleted()))
                .toList();

        Map<String, Object> summary = new HashMap<>();

        summary.put("totalInventoryItems", all.size());
        summary.put("totalQuantity", all.stream().mapToInt(Inventory::getQuantity).sum());

        Map<String, Integer> categoryTotals = new HashMap<>();
        for (Inventory inv : all) {
            categoryTotals.merge(inv.getCategory().name(), inv.getQuantity(), Integer::sum);
        }
        summary.put("quantityByCategory", categoryTotals);

        // ✅ low stock list only (no notification creation here)
        List<Map<String, Object>> lowStock = all.stream()
                .filter(i -> i.getQuantity() <= i.getMinThreshold())
                .map(i -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("itemName", i.getItemName());
                    m.put("quantity", i.getQuantity());
                    m.put("threshold", i.getMinThreshold());
                    return m;
                })
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

        List<Inventory> lowFeeds = repo.findAll().stream()
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
    public void adjustStock(String itemName, InventoryCategory category, int quantityChange, String unit, String note) {

        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("itemName is required");
        }

        String canonicalName = itemName.trim();

        Inventory inventory = repo.findByItemNameAndCategory(canonicalName, category)
                .filter(i -> !Boolean.TRUE.equals(i.getDeleted()))
                .orElseGet(() -> {
                    Inventory newInventory = new Inventory();
                    newInventory.setItemName(canonicalName);
                    newInventory.setCategory(category);
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

        repo.save(inventory);

        // ✅ Trigger low-stock notification ONLY when stock changes (here)
        if (inventory.getQuantity() <= inventory.getMinThreshold()) {

            String title = "Low Stock Alert: " + inventory.getItemName();
            String msg = "Item '" + inventory.getItemName() + "' is running low. (" +
                    inventory.getQuantity() + " " + inventory.getUnit() + " left)";

            // ✅ Don't repeat same alert within 24 hours
            LocalDateTime since = LocalDateTime.now().minusHours(24);

            if (!notificationRepo.existsByTitleAndMessageAndCreatedAtAfter(title, msg, since)) {
                notification.createNotification(
                        "GENERAL",
                        title,
                        msg,
                        null
                );
            }
        }
    }
}
