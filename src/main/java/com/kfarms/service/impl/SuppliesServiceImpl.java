package com.kfarms.service.impl;

import com.kfarms.dto.SuppliesRequestDto;
import com.kfarms.dto.SuppliesResponseDto;
import com.kfarms.entity.Inventory;
import com.kfarms.entity.InventoryCategory;
import com.kfarms.entity.Supplies;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.SuppliesMapper;
import com.kfarms.repository.SuppliesRepository;
import com.kfarms.service.InventoryService;
import com.kfarms.service.SuppliesService;
import jakarta.persistence.criteria.Predicate;
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
public class SuppliesServiceImpl implements SuppliesService {
    private final SuppliesRepository repo;
    private final InventoryService inventoryService;
    public SuppliesServiceImpl(SuppliesRepository repo, InventoryService inventoryService) {
        this.repo = repo;
        this.inventoryService = inventoryService;
    }

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
                    "Purchased from" + (entity.getSupplierName() != null ? entity.getSupplierName() : "Unknown supplier")
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
        Optional<Supplies> supplies = repo.findById(id);
        return supplies.map(SuppliesMapper::toResponseDto).orElse(null);
    }

    // UPDATE - update existing supply item by ID
    @Override
    public SuppliesResponseDto update(Long id, SuppliesRequestDto request, String updatedBy) {
        Supplies entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplies", "id", id));

        entity.setItemName(request.getItemName());
        entity.setSupplierName(request.getSupplierName());
        entity.setQuantity(request.getQuantity());
        entity.setUnitPrice(request.getUnitPrice());
        entity.setDate(request.getDate());
        entity.setUpdatedBy(updatedBy);

        repo.save(entity);
        return SuppliesMapper.toResponseDto(entity);
    }

    // DELETE - delete by ID
    @Override
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Supplies", "id", id);
        }
        repo.deleteById(id);
    }

    // SUMMARY
    @Override
    public Map<String, Object> getSummary() {
        List<Supplies> all = repo.findAll();
        Map<String, Object> summary = new HashMap<>();

        // Total Supply records
        summary.put("totalSupplies", all.size());

        // Total quantity purchased
        int totalQuantity = all.stream()
                .mapToInt(Supplies::getQuantity)
                .sum();
        summary.put("totalQuantity", totalQuantity);

        // total amount spent
        double totalAmount = all.stream()
                .mapToDouble(Supplies::getTotalPrice)
                .sum();
        summary.put("totalAmountSpent", totalAmount);

        // Amount spent by category
        Map<String, Double> amountByCategory = all.stream()
                        .collect(Collectors.groupingBy(s -> s.getCategory().name(),
                                Collectors.summingDouble(Supplies::getTotalPrice)));
        summary.put("amountByCategory", amountByCategory);

        // Quantity purchased by category
        Map<String, Integer> quantityByCategory = all.stream()
                        .collect(Collectors.groupingBy(s -> s.getCategory().name(),
                                Collectors.summingInt(Supplies::getQuantity)));
        summary.put("quantityByCategory", quantityByCategory);

        // Amount spent by supplier
        Map<String, Double> amountBySupplier = all.stream()
                        .filter(s -> s.getSupplierName() != null)
                        .collect(Collectors.groupingBy(Supplies::getSupplierName,
                                Collectors.summingDouble(Supplies::getTotalPrice)));
        summary.put("amountBySupplier", amountBySupplier);


        // last supply date
        all.stream()
                .map(Supplies::getDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .ifPresent(lastDate -> summary.put("lastSupplyDate", lastDate));

        return summary;
    }
}
