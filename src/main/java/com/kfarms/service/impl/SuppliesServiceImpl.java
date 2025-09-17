package com.kfarms.service.impl;

import com.kfarms.dto.SuppliesRequestDto;
import com.kfarms.dto.SuppliesResponseDto;
import com.kfarms.entity.Supplies;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.SuppliesMapper;
import com.kfarms.repository.SuppliesRepository;
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

@Service
public class SuppliesServiceImpl implements SuppliesService {
    private final SuppliesRepository repo;
    public SuppliesServiceImpl(SuppliesRepository repo) {this.repo = repo;}

    // CREATE - add new supply item
    @Override
    public SuppliesResponseDto save(SuppliesRequestDto dto) {
        Supplies entity = SuppliesMapper.toEntity(dto);
        Supplies saved = repo.save(entity);
        return SuppliesMapper.toResponseDto(saved);
    }

    // READ - get all with filtering & pagination
    @Override
    public Map<String, Object> getAll(int page, int size, String itemName, String supplier){
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Specification<Supplies> spec = (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          if (itemName != null && !itemName.isBlank()) {
              predicates.add(cb.like(cb.lower(root.get("itemName")), "%" + itemName.toLowerCase() + "%"));
          }
          if (supplier != null && !supplier.isBlank()) {
              predicates.add(cb.like(cb.lower(root.get("supplier")), "%" + supplier.toLowerCase() + "%"));
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

        // total records
        summary.put("totalSupplies", all.size());

        // total quantity purchased
        int totaQuantity = all.stream()
                .mapToInt(Supplies::getQuantity).sum();
        summary.put("totalQuantity", totaQuantity);

        // total amount spent
        double totalAmount = all.stream()
                .mapToDouble(s -> s.getUnitPrice() * s.getQuantity())
                .sum();

        // last supply date
        all.stream()
                .map(Supplies::getDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .ifPresent(lastDate -> summary.put("lastSupplyDate", lastDate));

        return summary;
    }
}
