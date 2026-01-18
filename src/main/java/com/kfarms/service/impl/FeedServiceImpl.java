package com.kfarms.service.impl;

import com.kfarms.dto.FeedRequestDto;
import com.kfarms.dto.FeedResponseDto;
import com.kfarms.entity.Feed;
import com.kfarms.entity.FeedBatchType;
import com.kfarms.entity.InventoryCategory;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.FeedMapper;
import com.kfarms.repository.FeedRepository;
import com.kfarms.service.FeedService;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {
    private final FeedRepository repo;
    private final InventoryService inventoryService;
    private final NotificationService notification;


    // CREATE - add new feed
    @Override
    public FeedResponseDto create(FeedRequestDto dto) {
        Feed entity = FeedMapper.toEntity(dto);
        Feed saved = repo.save(entity);

        // auto update inventory after create
        inventoryService.adjustStock(
                saved.getFeedName(),
                InventoryCategory.FEED,
                -saved.getQuantityUsed(),
                "kg",
                "Consumed by batch" + saved.getBatchId()
        );
        return FeedMapper.toResponseDto(saved);
    }

    // READ - get all with filtering & pagination
    @Override
    public Map<String, Object> getAll(int page, int size, String batchType, LocalDate date) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());


        // Convert batchType string to enum (case-insensitive)
        FeedBatchType batchTypeEnum = null;
        if (batchType != null && !batchType.isBlank()) {
            try {
                batchTypeEnum = FeedBatchType.valueOf(batchType.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "Invalid batchType: '" + batchType + "' . Allowed values: "
                        + Arrays.toString(FeedBatchType.values())
                );
            }
        }

        final FeedBatchType batchTypeEnumFinal = batchTypeEnum;

        Specification<Feed> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (batchTypeEnumFinal != null) {
                predicates.add(cb.equal(root.get("batchType"), batchTypeEnumFinal));
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

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("page", feedPage.getNumber());
        result.put("size", feedPage.getSize());
        result.put("totalItems", feedPage.getTotalElements());
        result.put("totalPages", feedPage.getTotalPages());
        result.put("hasNext", feedPage.hasNext());
        result.put("hasPrevious", feedPage.hasPrevious());

        return result;
    }

    // READ - get by ID
    @Override
    public FeedResponseDto getById(Long id) {
        Feed entity = repo.findById(id)
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Feed", "id", id));
        return FeedMapper.toResponseDto(entity);
    }

    // UPDATE - update existing Feed by ID
    @Override
    public FeedResponseDto update(Long id, FeedRequestDto request, String updatedBy) {
        Feed entity = repo.findById(id)
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Feed", "id", id));

        // update fields from request
        entity.setBatchType(FeedBatchType.valueOf(request.getBatchType().toUpperCase()));
        entity.setFeedName(request.getFeedName());
        entity.setBatchId(request.getBatchId());
        entity.setNote(request.getNote());
        entity.setQuantityUsed(request.getQuantityUsed());
        entity.setUpdatedBy(updatedBy);

        repo.save(entity);
        return FeedMapper.toResponseDto(entity);
    }

    // DELETE - delete by ID
    @Override
    public void delete(Long id, String deletedBy) {
        Feed entity = repo.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Feed", "id", id));

        if (Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Feed record with ID " + id + " has already been deleted");
        }
        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setUpdatedBy(deletedBy);
        repo.save(entity);
    }

    // RESTORE
    @Override
    public void restore(Long id) {
        Feed entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feed", "id", id));

        if (!Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Feed with ID " + id + " has already been restored");
        }

        entity.setDeleted(false);
        entity.setDeletedAt(LocalDateTime.now());
        repo.save(entity);
    }

    // SUMMARY
    @Override
    public Map<String, Object> getSummary() {
        List<Feed> all = repo.findAll()
                .stream()
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .toList();

        Map<String, Object> summary = new HashMap<>();

        // total feed batches (records)
        summary.put("totalFeeds", all.size());

        // total quantity of all feeds
        int totalQuantityUsed = all.stream()
                .mapToInt(f -> f.getQuantityUsed() != null ? f.getQuantityUsed() : 0)
                .sum();
        summary.put("totalQuantityUsed", totalQuantityUsed);

        // count by type
        Map<String, Long> countByType = all.stream()
                .collect(Collectors.groupingBy(f -> f.getBatchType().name(), Collectors.counting()));
        summary.put("countByType", countByType);

        // sum quantity by type
        Map<String, Integer> quantityByType = all.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getBatchType().name(),
                        Collectors.summingInt(f -> f.getQuantityUsed() != null ? f.getQuantityUsed() : 0)
                ));
        int grandTotal = quantityByType.values()
                        .stream()
                                .mapToInt(Integer::intValue)
                                        .sum();

        List<Map<String, Object>> breakdown = new ArrayList<>();

        if(grandTotal > 0) {
            quantityByType.forEach((type, qty) -> {
                double percentage = (qty * 100.0) / grandTotal;

                String label;
                switch (type) {
                    case "LAYERS":
                        label = "Poultry";
                        break;
                    case "FISH":
                        label = "Fish";
                        break;
                    case "DUCKS":
                        label = "Ducks";
                        break;
                    default:
                        label = "others";
                }

                Map<String, Object> entry = new HashMap<>();
                entry.put("label", label);
                entry.put("value", Math.round(percentage)); // round to whole %
                breakdown.add(entry);
            });
        }
        summary.put("feedBreakdown", breakdown);

        // sum quantity used per month
        int usedThisMonth = all.stream()
                .filter(f -> f.getDate() != null && f.getDate().getMonth().equals(LocalDate.now().getMonth()))
                .mapToInt(f -> f.getQuantityUsed() != null ? f.getQuantityUsed() : 0)
                .sum();
        summary.put("usedThisMonth", usedThisMonth);

        // last feed added date
        all.stream()
                .map(Feed::getCreatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .ifPresent(lastDate -> summary.put("lastFeedDate", lastDate));
        if (usedThisMonth > 100) {
            notification.createNotification(
                    "FEED",
                    "High Feed Usage",
                    "Feed usage for this month has exceeded 100kg",
                    null
            );
        }

        if (totalQuantityUsed < 100) {
            notification.createNotification(
                    "FEED",
                    "Low Feed Activity",
                    "Feed consumption appears unusually low this month",
                    null
            );
        }

        return summary;
    }

}
