package com.kfarms.service.impl;

import com.kfarms.dto.SalesRequestDto;
import com.kfarms.dto.SalesResponseDto;
import com.kfarms.entity.InventoryCategory;
import com.kfarms.entity.Sales;
import com.kfarms.entity.SalesCategory;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.SalesMapper;
import com.kfarms.repository.SalesRepository;
import com.kfarms.service.InventoryService;
import com.kfarms.service.NotificationService;
import com.kfarms.service.SalesService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesServiceImpl implements SalesService {

    private final SalesRepository salesRepo;
    private final InventoryService inventoryService;
    private final NotificationService notification;
    private final TenantRepository tenantRepository;


    // CREATE - add a new sale item
    @Override
    public SalesResponseDto create(SalesRequestDto dto) {
        Long tenantId = requireTenantId();
        Sales entity = SalesMapper.toEntity(dto);
        entity.setTenant(resolveTenant(tenantId));
        Sales saved = salesRepo.save(entity);
        return SalesMapper.toResponseDto(saved);
    }

    // READ - get all sales with pagination & filters
    @Override
    public Map<String, Object> getAll(int page, int size, String itemName, String category, LocalDate date, Boolean deleted) {
        Long tenantId = requireTenantId();

        Sort sort = Boolean.TRUE.equals(deleted)
                ? Sort.by(Sort.Direction.DESC, "deletedAt")
                .and(Sort.by(Sort.Direction.DESC, "id"))
                : Sort.by(Sort.Direction.DESC, "id");

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Sales> spec = (root, query, cb) -> {

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
          if (date != null) {
              predicates.add(cb.equal(root.get("salesDate"), date));
          }
          return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Sales> salesPage = salesRepo.findAll(spec, pageable);
        List<SalesResponseDto> items = salesPage
                .getContent()
                .stream()
                .map(SalesMapper::toResponseDto)
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("page", salesPage.getNumber());
        result.put("size", salesPage.getSize());
        result.put("totalItems", salesPage.getTotalElements());
        result.put("totalPages", salesPage.getTotalPages());
        result.put("hasNext", salesPage.hasNext());
        result.put("hasPrevious", salesPage.hasPrevious());

        return result;
    }

    // READ - get sale item by ID
    @Override
    public SalesResponseDto getById(Long id) {
        Long tenantId = requireTenantId();
        Sales entity = salesRepo.findByIdAndTenant_Id(id, tenantId)
                .filter(s -> !Boolean.TRUE.equals(s.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Sales", "id", id));

        return SalesMapper.toResponseDto(entity);
    }

    // UPDATE - update existing sales item by ID
    @Override
    public SalesResponseDto update(Long id, SalesRequestDto request, String updatedBy) {
        Long tenantId = requireTenantId();
        Sales entity = salesRepo.findByIdAndTenant_Id(id, tenantId)
                .filter(s -> !Boolean.TRUE.equals(s.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Sales", "id", id));

        entity.setItemName(request.getItemName());
        entity.setCategory(SalesCategory.valueOf(request.getCategory().toUpperCase()));
        entity.setQuantity(request.getQuantity());
        entity.setUnitPrice(request.getUnitPrice());
        entity.setBuyer(request.getBuyer());
        entity.setNote(request.getNote());
        entity.setSalesDate(request.getSalesDate() != null ? request.getSalesDate() : entity.getSalesDate());
        entity.setTotalPrice(calculateTotalPrice(request.getQuantity(), request.getUnitPrice()));
        entity.setUpdatedBy(updatedBy);

        salesRepo.save(entity);
        return SalesMapper.toResponseDto(entity);
    }

    // DELETE - delete sales item by ID
    @Override
    public void delete(Long id, String deletedBy) {
        Long tenantId = requireTenantId();
        Sales entity = salesRepo.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales", "id", id));

        if (Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Sales record with ID " + id + " has already been deleted");
        }

        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setUpdatedBy(deletedBy);
        salesRepo.save(entity);
    }

    // DELETE (permanent)
    @Override
    public void permanentDelete(Long id, String deletedBy) {
        Long tenantId = requireTenantId();
        Sales entity = salesRepo.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales", "id", id));
        salesRepo.delete(entity);
    }

    // RESTORE
    @Override
    public void restore(Long id) {
        Long tenantId = requireTenantId();
        Sales entity = salesRepo.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales", "id", id));

        if (!Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Sales record with ID " + id + " has already been restored");
        }

        entity.setDeleted(false);
        entity.setDeletedAt(null);
        salesRepo.save(entity);
    }

    // SUMMARY - for analysis, dashboard and reports
    @Override
    public Map<String, Object> getSummary() {

        Long tenantId = TenantContext.getTenantId();

        List<Sales> all = salesRepo.findAllActiveByTenantId(tenantId);

        Map<String, Object> summary = new HashMap<>();

        LocalDate today = LocalDate.now();
        LocalDate lastMonth = today.minusMonths(1);
        LocalDate sevenDaysAgo = today.minusDays(7);

        // 🟣 Total sales record
        summary.put("totalSalesRecords", all.size());

        /* 🟣 All-time revenue */
        BigDecimal totalRevenueAllTime = all.stream()
                .map(Sales::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("totalRevenue", totalRevenueAllTime);

        /* 🟣 Revenue today */
        BigDecimal revenueToday = all.stream()
                .filter(s -> s.getSalesDate() != null &&
                        s.getSalesDate().isEqual(today))
                .map(Sales::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("revenueToday", revenueToday);

        /* 🟣 Revenue this month */
        BigDecimal revenueThisMonth = all.stream()
                .filter(s -> s.getSalesDate() != null &&
                        s.getSalesDate().getMonth() == today.getMonth() &&
                        s.getSalesDate().getYear() == today.getYear())
                .map(Sales::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("revenueThisMonth", revenueThisMonth);

        /* 🟣 Revenue this year (ANNUAL) */
        BigDecimal revenueThisYear = all.stream()
                .filter(s -> s.getSalesDate() != null &&
                        s.getSalesDate().getYear() == today.getYear())
                .map(Sales::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("revenueThisYear", revenueThisYear);

        /* 🟣 Revenue last month */
        BigDecimal revenueLastMonth = all.stream()
                .filter(s -> s.getSalesDate() != null &&
                        s.getSalesDate().getMonth() == lastMonth.getMonth() &&
                        s.getSalesDate().getYear() == lastMonth.getYear())
                .map(Sales::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("revenueLastMonth", revenueLastMonth);

        /* 🟣 Count by category */
        Map<String, Long> countByCategory = all.stream()
                .filter(s -> s.getCategory() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getCategory().name(),
                        Collectors.counting()
                ));

        /* 🟣 Revenue by category */
        Map<String, BigDecimal> revenueByCategory = all.stream()
                .filter(s -> s.getCategory() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getCategory().name(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Sales::getTotalPrice,
                                BigDecimal::add
                        )
                ));
        summary.put("revenueByCategory", revenueByCategory);

        /* 🟣 Last sales date */
        all.stream()
                .map(Sales::getSalesDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .ifPresent(d -> summary.put("lastSalesDate", d));




        // 🟣 Compare and Notify if there’s a drop
        if (revenueLastMonth.compareTo(BigDecimal.ZERO) > 0 &&
        revenueThisMonth.compareTo(revenueLastMonth) < 0) {
            notification.createNotification(
                    tenantId,
                    "FINANCE",
                    "Revenue Drop Alert",
                    "This month's revenue (" + revenueThisMonth + ") is lower than last month " + revenueLastMonth + ").",
                    null
            );
        }

        return summary;
    }

    private BigDecimal calculateTotalPrice(Integer quantity, BigDecimal unitPrice) {
        if (quantity == null || unitPrice == null) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity.longValue()));
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
}
