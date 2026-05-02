package com.kfarms.service.impl;

import com.kfarms.dto.SuppliesRequestDto;
import com.kfarms.dto.SuppliesResponseDto;
import com.kfarms.entity.InventoryCategory;
import com.kfarms.entity.Supplies;
import com.kfarms.entity.SupplyCategory;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.SuppliesMapper;
import com.kfarms.repository.SuppliesRepository;
import com.kfarms.service.InventoryService;
import com.kfarms.service.NotificationService;
import com.kfarms.service.SuppliesService;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.repository.TenantRepository;
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
import java.math.RoundingMode;
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
    private final TenantRepository tenantRepository;
    private final TenantRecordAuditService tenantRecordAuditService;


    // CREATE - add new supply item
    @Override
    public SuppliesResponseDto create(SuppliesRequestDto dto) {
        Long tenantId = requireTenantId();
        Supplies entity = SuppliesMapper.toEntity(dto);
        entity.setTenant(resolveTenant(tenantId));
        Supplies saved = repo.save(entity);
        tenantRecordAuditService.created(
                tenantId,
                entity.getCreatedBy(),
                "SUPPLIES",
                saved.getId(),
                suppliesTargetName(saved),
                suppliesSummary(saved),
                "Created supply record for " + suppliesTargetName(saved) + "."
        );

        // auto update inventory if not livestock

        if (saved.getCategory() != null && !saved.getCategory().name().equalsIgnoreCase("LIVESTOCK")) {
            try {
                inventoryService.adjustStock(
                        saved.getItemName(),
                        InventoryCategory.valueOf(saved.getCategory().name()),
                        saved.getQuantity(),
                        "units",
                        "Purchased from " + (entity.getSupplierName() != null ? entity.getSupplierName() : "Unknown supplier")
                                + (entity.getNote() != null ? " | Note: " + entity.getNote() : "")
                );
            } catch (IllegalArgumentException ex) {
                // Category not present in InventoryCategory (e.g., LIVESTOCK) — skip inventory update
                //log.warn("Skipping inventory update for supply category: {}", saved.getCategory());
            }
        }


        return SuppliesMapper.toResponseDto(saved);
    }

    // READ - get all with filtering & pagination
    @Override
    public Map<String, Object> getAll(int page, int size, String itemName, String category, LocalDate supplyDate, Boolean deleted){
        Long tenantId = requireTenantId();

        Sort sort = Boolean.TRUE.equals(deleted)
                ? Sort.by(Sort.Direction.DESC, "deletedAt")
                .and(Sort.by(Sort.Direction.DESC, "id"))
                : Sort.by(Sort.Direction.DESC, "id");

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Supplies> spec = (root, query, cb) -> {

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("tenant").get("id"), tenantId));

        if (deleted != null) {
            predicates.add(cb.equal(root.get("deleted"), deleted));
        }

          if (itemName != null && !itemName.isBlank()) {
              predicates.add(cb.like(cb.lower(root.get("itemName")), "%" + itemName.toLowerCase() + "%"));
          }
          if (category != null && !category.isBlank()) {
              predicates.add(cb.like(cb.lower(root.get("category")), "%" + category.toLowerCase() + "%"));
          }
          if (supplyDate != null) {
              predicates.add(cb.equal(root.get("supplyDate"), supplyDate));
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
        Long tenantId = requireTenantId();
        Optional<Supplies> supplies = repo.findByIdAndTenant_Id(id, tenantId)
                .filter(s -> !Boolean.TRUE.equals(s.getDeleted()));

        return supplies.map(SuppliesMapper::toResponseDto).orElse(null);
    }

    // UPDATE - update existing supply item by ID
    @Override
    public SuppliesResponseDto update(Long id, SuppliesRequestDto request, String updatedBy) {
        Long tenantId = requireTenantId();
        Supplies entity = repo.findByIdAndTenant_Id(id, tenantId)
                .filter(s -> !Boolean.TRUE.equals(s.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Supplies", "id", id));
        String previousSummary = suppliesSummary(entity);

        entity.setItemName(request.getItemName());
        entity.setSupplierName(request.getSupplierName());
        if (request.getCategory() != null) {
            entity.setCategory(SupplyCategory.valueOf(request.getCategory().toUpperCase(Locale.ROOT)));
        }
        entity.setQuantity(request.getQuantity());
        entity.setUnitPrice(request.getUnitPrice());
        entity.setTotalPrice(calculateTotalPrice(request.getQuantity(), request.getUnitPrice()));
        entity.setSupplyDate(request.getSupplyDate() != null ? request.getSupplyDate() : entity.getSupplyDate());
        entity.setNote(request.getNote());
        entity.setUpdatedBy(updatedBy);

        repo.save(entity);
        tenantRecordAuditService.updated(
                tenantId,
                updatedBy,
                "SUPPLIES",
                entity.getId(),
                suppliesTargetName(entity),
                previousSummary,
                suppliesSummary(entity),
                "Updated supply record for " + suppliesTargetName(entity) + "."
        );
        return SuppliesMapper.toResponseDto(entity);
    }

    // DELETE - delete by ID
    @Override
    public void delete(Long id, String deletedBy) {
        Long tenantId = requireTenantId();
        Supplies entity = repo.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplies", "id", id));
        String previousSummary = suppliesSummary(entity);

        if (Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Supply record with ID " + id + " has already been deleted");
        }

        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setUpdatedBy(deletedBy);
        repo.save(entity);
        tenantRecordAuditService.deleted(
                tenantId,
                deletedBy,
                "SUPPLIES",
                entity.getId(),
                suppliesTargetName(entity),
                previousSummary,
                "Deleted supply record for " + suppliesTargetName(entity) + "."
        );
    }

    // DELETE (permanently)
    @Override
    public void permanentDelete(Long id, String deletedBy) {
        Long tenantId = requireTenantId();
        Supplies entity = repo.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplies", "id", id));
        repo.delete(entity);
    }

    // RESTORE
    @Override
    public void restore(Long id) {
        Long tenantId = requireTenantId();
        Supplies entity = repo.findByIdAndTenant_Id(id, tenantId)
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

        Long tenantId = TenantContext.getTenantId();

        LocalDate today = LocalDate.now();

        List<Supplies> all = repo.findAllActiveByTenantId(tenantId);

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

        // 🟣 Amount spend daily
        BigDecimal spentToday = all.stream()
                .filter(s -> s.getSupplyDate() != null && s.getSupplyDate().isEqual(today))
                .map(Supplies::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("spentToday", spentToday);

        // 🟣 Amount spend monthly
        BigDecimal spentThisMonth = all.stream()
                .filter(s -> s.getSupplyDate() != null &&
                        s.getSupplyDate().getMonth() == today.getMonth() &&
                        s.getSupplyDate().getYear() == today.getYear())
                .map(Supplies::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("spentThisMonth", spentThisMonth);

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
                    tenantId,
                    "SUPPLIES",
                    "Low Supply Stock",
                    "Overall supplies are running low. Current total quantity: " + totalQuantity,
                    null
            );
        }

        BigDecimal limit = new BigDecimal("500000");
        if (totalAmount.compareTo(limit) > 0) {
            notification.createNotification(
                    tenantId,
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
                                tenantId,
                                "SUPPLIES",
                                "No Recent Supply",
                                "No new supplies have been recorded since " + lastDate,
                                null
                        );
                    }
                });


        return summary;
    }

    private BigDecimal calculateTotalPrice(Integer quantity, BigDecimal unitPrice) {
        if (quantity == null || unitPrice == null) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity.longValue())).setScale(2, RoundingMode.HALF_UP);
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

    private String suppliesTargetName(Supplies supplies) {
        String itemName = supplies != null ? supplies.getItemName() : null;
        return itemName != null && !itemName.isBlank() ? itemName.trim() : "Supply record";
    }

    private String suppliesSummary(Supplies supplies) {
        if (supplies == null) {
            return "";
        }
        return String.format(
                "Qty %s • Unit %s • Total %s • Date %s",
                Optional.ofNullable(supplies.getQuantity()).orElse(0),
                formatAmount(supplies.getUnitPrice()),
                formatAmount(supplies.getTotalPrice()),
                Optional.ofNullable(supplies.getSupplyDate()).map(LocalDate::toString).orElse("N/A")
        );
    }

    private String formatAmount(BigDecimal amount) {
        return amount != null ? amount.toPlainString() : "0";
    }
}
