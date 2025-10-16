package com.kfarms.service.impl;

import com.kfarms.dto.InventoryRequestDto;
import com.kfarms.dto.InventoryResponseDto;
import com.kfarms.entity.Inventory;
import com.kfarms.entity.InventoryCategory;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.InventoryMapper;
import com.kfarms.repository.InventoryRepository;
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

    // CREATE - add new inventory item
    @Override
    public InventoryResponseDto create(InventoryRequestDto dto) {
        Inventory entity = InventoryMapper.toEntity(dto);
        Inventory saved = repo.save(entity);
        return InventoryMapper.toResponseDto(saved);
    }

    // READ - get all inventory items with Pagination and Filters
    @Override
    public Map<String, Object> getAll(int page, int size, String itemName, String category, LocalDate lastUpdated) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Specification<Inventory> spec = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // always exclude deleted
            predicates.add(cb.isFalse(root.get("deleted")));


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

    // READ - get inventory item by ID
    @Override
    public InventoryResponseDto getById(Long id) {
        Optional<Inventory> inventory = repo.findById(id)
                .filter(i -> !Boolean.TRUE.equals(i.getDeleted()));

        return inventory.map(InventoryMapper::toResponseDto).orElse(null);
    }

    // UPDATE - update existing inventory by ID
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

    // DELETE - delete existing inventory item by ID
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

    // RESTORE
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

    // SUMMARY - Dashboard, Report and Analysis
    @Override
    public Map<String, Object> getSummary() {
        List<Inventory> all = repo.findAll()
                .stream()
                .filter(i -> !Boolean.TRUE.equals(i.getDeleted()))
                .toList();

        Map<String, Object> summary = new HashMap<>();

        // Total Inventory record
        summary.put("totalInventoryItems", all.size());

        // Total Quantity
        summary.put("totalQuantity", all.stream().mapToInt(Inventory::getQuantity).sum());

        // breakdown by category
        Map<String, Integer> categoryTotals = new HashMap<>();
        for (Inventory inv : all) {
            categoryTotals.merge(inv.getCategory().name(), inv.getQuantity(), Integer::sum);
        }
        summary.put("quantityByCategory", categoryTotals);

        // low stock
        List<Map<String, Object>> lowStock = all.stream()
                .filter(i -> i.getQuantity() <= i.getMinThreshold())
                .map(i -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("itemName", i.getItemName());
                    m.put("quantity", i.getQuantity());
                    m.put("threshold", i.getMinThreshold());

                    // 🟣 Trigger notification when item is below threshold
                    notification.createNotification(
                            "GENERAL",
                            "Low Stock Alert: " + i.getItemName(),
                            "Item '" + i.getItemName() + "' is running low. (" + i.getQuantity() + " " + i.getUnit() + " left)"
                    );
                    return m;
                })
                .toList();
        summary.put("lowStockItems", lowStock);

        // last updated
        all.stream()
                .map(Inventory::getLastUpdated)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .ifPresent(last -> summary.put("lastUpdated", last));

        return summary;
    }

    // Auto update inventory
    @Override
    public void adjustStock(String itemName, InventoryCategory category, int quantityChange, String unit, String note){
        Inventory inventory = repo.findByItemNameAndCategory(itemName, category)
                .filter(i -> !Boolean.TRUE.equals(i.getDeleted()))
                .orElseGet(() -> {
                   Inventory newInventory = new Inventory();
                   newInventory.setItemName(itemName.trim());
                   newInventory.setCategory(category);
                   newInventory.setQuantity(0);
                   newInventory.setUnit(unit);
                   newInventory.setNote(note);
                   return newInventory;
                });

        // Apply stock adjustment
        inventory.setQuantity(inventory.getQuantity() + quantityChange);
        inventory.setLastUpdated(LocalDate.now());

        // Prevent negative stock
        if (inventory.getQuantity() < 0) {
            throw new IllegalArgumentException("Insufficient stock for " + itemName);
        }

        repo.save(inventory);
    }
}
