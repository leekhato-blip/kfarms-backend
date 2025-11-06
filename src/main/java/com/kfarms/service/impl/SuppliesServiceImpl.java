package com.kfarms.service.impl;

import com.kfarms.dto.SuppliesRequestDto;
import com.kfarms.dto.SuppliesResponseDto;
import com.kfarms.entity.AppUser;
import com.kfarms.entity.InventoryCategory;
import com.kfarms.entity.Role;
import com.kfarms.entity.Supplies;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.SuppliesMapper;
import com.kfarms.repository.SuppliesRepository;
import com.kfarms.service.InventoryService;
import com.kfarms.service.NotificationService;
import com.kfarms.service.SuppliesService;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuppliesServiceImpl implements SuppliesService {

    private final SuppliesRepository repo;
    private final InventoryService inventoryService;
    private final NotificationService notification;


    // CREATE - add new supply item
    @Override
    public SuppliesResponseDto create(SuppliesRequestDto dto) {
        Supplies entity = SuppliesMapper.toEntity(dto);
        Supplies saved = repo.save(entity);

        // auto update inventory if not livestock
        if (saved.getCategory() != null && !saved.getCategory().name().equalsIgnoreCase("LIVESTOCK")) {
            inventoryService.adjustStock(
                    saved.getItemName(),
                    InventoryCategory.valueOf(saved.getCategory().name()),
                    saved.getQuantity(),
                    "units",
                    "Purchased from " + (entity.getSupplierName() != null ? entity.getSupplierName() : "Unknown supplier")
                     + (entity.getNote() != null ? " | Note: " + entity.getNote() : "")
            );
        }
        return SuppliesMapper.toResponseDto(saved);
    }

    // READ - get all with filtering & pagination
    @Override
    public Map<String, Object> getAll(int page, int size, String itemName, String category, LocalDate date){
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Specification<Supplies> spec = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

          if (itemName != null && !itemName.isBlank()) {
              predicates.add(cb.like(cb.lower(root.get("itemName")), "%" + itemName.toLowerCase() + "%"));
          }
          if (category != null && !category.isBlank()) {
              predicates.add(cb.like(cb.lower(root.get("category")), "%" + category.toLowerCase() + "%"));
          }
          if (date != null) {
              predicates.add(cb.equal(root.get("date"), date));
          }
          return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Supplies> supplyPage = repo.findAll(spec, pageable);
        List<SuppliesResponseDto> items = supplyPage.getContent().stream()
                .map(SuppliesMapper::toResponseDto)
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("page", supplyPage.getNumber());
        result.put("size", supplyPage.getSize());
        result.put("totalItems", supplyPage.getTotalElements());
        result.put("totalPages", supplyPage.getTotalPages());
        result.put("hasNext", supplyPage.hasNext());
        result.put("hasPrevious", supplyPage.hasPrevious());

        return result;
    }

    // READ - get by ID
    @Override
    public SuppliesResponseDto getById(Long id){
        Optional<Supplies> supplies = repo.findById(id)
                .filter(s -> !Boolean.TRUE.equals(s.getDeleted()));

        return supplies.map(SuppliesMapper::toResponseDto).orElse(null);
    }

    // UPDATE - update existing supply item by ID
    @Override
    public SuppliesResponseDto update(Long id, SuppliesRequestDto request, String updatedBy) {
        Supplies entity = repo.findById(id)
                .filter(s -> !Boolean.TRUE.equals(s.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Supplies", "id", id));

        entity.setItemName(request.getItemName());
        entity.setSupplierName(request.getSupplierName());
        entity.setQuantity(request.getQuantity());
        entity.setUnitPrice(request.getUnitPrice());
        entity.setSupplyDate(request.getSupplyDate());
        entity.setUpdatedBy(updatedBy);

        repo.save(entity);
        return SuppliesMapper.toResponseDto(entity);
    }

    // DELETE - delete by ID
    @Override
    public void delete(Long id, String deletedBy) {
        Supplies entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplies", "id", id));

        if (Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Supply record with ID " + id + " has already been deleted");
        }

        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setUpdatedBy(deletedBy);
        repo.save(entity);
    }

    // RESTORE
    @Override
    public void restore(Long id) {
        Supplies entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplies", "id", id));

        if (!Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Supply record with ID " + id + " has already been restored");
        }

        entity.setDeleted(false);
        entity.setDeletedAt(null);
        repo.save(entity);
    }

    // SUMMARY
    @Override
    public Map<String, Object> getSummary() {
        List<Supplies> all = repo.findAll()
                .stream()
                .filter(s -> !Boolean.TRUE.equals(s.getDeleted()))
                .toList();

        Map<String, Object> summary = new HashMap<>();

        // 🟣 Total Supply records
        summary.put("totalSupplies", all.size());

        // 🟣 Total quantity purchased
        int totalQuantity = all.stream()
                .mapToInt(Supplies::getQuantity)
                .sum();
        summary.put("totalQuantity", totalQuantity);

        // 🟣 Total amount spent
        BigDecimal totalAmount = all.stream()
                .map(Supplies::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("totalAmountSpent", totalAmount);

        // 🟣 Amount spent by category
        Map<String, BigDecimal> amountByCategory = all.stream()
                        .filter(s -> s.getCategory() != null)
                                .collect(Collectors.groupingBy(
                                        s -> s.getCategory().name(),
                                        Collectors.reducing(BigDecimal.ZERO, Supplies::getTotalPrice, BigDecimal::add)
                                ));
        summary.put("amountByCategory", amountByCategory);

        // 🟣 Quantity purchased by category
        Map<String, Integer> quantityByCategory = all.stream()
                        .filter(s -> s.getCategory() != null)
                                .collect(Collectors.groupingBy(
                                        s -> s.getCategory().name(),
                                        Collectors.summingInt(Supplies::getQuantity)
                                ));
        summary.put("quantityByCategory", quantityByCategory);

        // 🟣 Amount spent by supplier
        Map<String, BigDecimal> amountBySupplier = all.stream()
                        .filter(s -> s.getSupplierName() != null)
                        .collect(Collectors.groupingBy(
                                Supplies::getSupplierName,
                                Collectors.reducing(BigDecimal.ZERO, Supplies::getTotalPrice, BigDecimal::add)
                        ));

        summary.put("amountBySupplier", amountBySupplier);

        // ==== ALERTS ====
        if (totalQuantity < 10) {
            notification.createNotification(
                    "SUPPLIES",
                    "Low Supply Stock",
                    "Overall supplies are running low. Current total quantity: " + totalQuantity,
                    null
            );
        }

        BigDecimal limit = new BigDecimal("500000");
        if (totalAmount.compareTo(limit) > 0) {
            notification.createNotification(
                    "FINANCE",
                    "High Supply Expenses",
                    "This month's total expenses on supplies have exceeded ₦500,000",
                    null
            );
        }

        // 🟣 last supply date
        all.stream()
                .map(Supplies::getSupplyDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .ifPresent(lastDate -> {
                    if (lastDate.isBefore(LocalDate.now().minusDays(30))) {
                        notification.createNotification(
                                "SUPPLIES",
                                "No Recent Supply",
                                "No new supplies have been recorded since " + lastDate,
                                null
                        );
                    }
                });


        return summary;
    }
}
