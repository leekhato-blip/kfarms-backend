package com.kfarms.service.impl;

import com.kfarms.dto.SalesRequestDto;
import com.kfarms.dto.SalesResponseDto;
import com.kfarms.entity.Sales;
import com.kfarms.entity.SalesCategory;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.SalesMapper;
import com.kfarms.repository.SalesRepository;
import com.kfarms.service.SalesService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalesServiceImpl implements SalesService {
    private final SalesRepository repo;
    public SalesServiceImpl(SalesRepository repo){this.repo = repo;}

    // CREATE - add a new sale item
    @Override
    public SalesResponseDto create(SalesRequestDto dto) {
        Sales entity = SalesMapper.toEntity(dto);
        Sales saved = repo.save(entity);
        return SalesMapper.toResponseDto(saved);
    }

    // READ - get all with pagination & filtering
    @Override
    public Map<String, Object> getAll(int page, int size, String itemName, String category, LocalDate date) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Specification<Sales> spec = (root, query, cb) -> {
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

        Page<Sales> salesPage = repo.findAll(spec, pageable);
        List<SalesResponseDto> items = salesPage.getContent().stream()
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
        Optional<Sales> sales = repo.findById(id);
        return sales.map(SalesMapper::toResponseDto).orElse(null);
    }

    // UPDATE - update existing sales item by ID
    @Override
    public SalesResponseDto update(Long id, SalesRequestDto request, String updatedBy) {
        Sales entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales", "id", id));

        entity.setItemName(request.getItemName());
        entity.setCategory(SalesCategory.valueOf(request.getCategory().toUpperCase()));
        entity.setQuantity(request.getQuantity());
        entity.setUnitPrice(request.getUnitPrice());
        entity.setBuyer(request.getBuyer());
        entity.setUpdatedBy(updatedBy);

        repo.save(entity);
        return SalesMapper.toResponseDto(entity);
    }

    // DELETE - delete sales item by ID
    @Override
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Sales", "id", id);
        }
        repo.deleteById(id);
    }

    // SUMMARY - for analysis, dashboard and reports
    @Override
    public Map<String, Object> getSummary() {
        List<Sales> all = repo.findAll();
        Map<String, Object> summary = new HashMap<>();

        // Total sales record
        summary.put("totalSalesRecords", all.size());

        // Total revenue
        double totalRevenue = all.stream()
                .mapToDouble(Sales::getTotalPrice)
                .sum();
        summary.put("totalRevenue", totalRevenue);

        // Total by category
        Map<String, Long> countByCategory = all.stream()
                .collect(Collectors.groupingBy(s -> s.getCategory().name(), Collectors.counting()));
        summary.put("countByCategory", countByCategory);

        // Total revenue by category
        Map<String, Double> revenueByCategory = all.stream()
                .collect(Collectors.groupingBy(s -> s.getCategory().name(),
                        Collectors.summingDouble(Sales::getTotalPrice)));
        summary.put("revenueByCategory", revenueByCategory);

        // Monthly Revenue
        double revenueThisMonth = all.stream()
                .filter(s -> s.getDate() != null &&
                             s.getDate().getMonth() == LocalDate.now().getMonth() &&
                             s.getDate().getYear() == LocalDate.now().getYear())
                .mapToDouble(Sales::getTotalPrice)
                .sum();
        summary.put("revenueThisMonth", revenueThisMonth);

        // Annual Revenue
        double revenueThisYear = all.stream()
                .filter(s -> s.getDate() != null && s.getDate().getYear() == (LocalDate.now().getYear()))
                .mapToDouble(Sales::getTotalPrice)
                .sum();
        summary.put("revenueThisYear", revenueThisYear);

        all.stream()
                .map(Sales::getDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .ifPresent(last -> summary.put("lastSalesDate", last));

        return summary;
    }
}
