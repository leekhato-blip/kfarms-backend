package com.kfarms.service.impl;

import com.kfarms.dto.FeedRequestDto;
import com.kfarms.dto.FeedResponseDto;
import com.kfarms.entity.Feed;
import com.kfarms.entity.FeedBatchType;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.FeedMapper;
import com.kfarms.repository.FeedRepository;
import com.kfarms.service.FeedService;
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
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {
    private final FeedRepository repo;

    // CREATE - add new feed
    @Override
    public FeedResponseDto create(FeedRequestDto dto) {
        Feed entity = FeedMapper.toEntity(dto);
        Feed saved = repo.save(entity);
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
                System.out.println("Invalid batchType detected: " + batchType); // debug
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
        Optional<Feed> feed = repo.findById(id);
        return feed.map(FeedMapper::toResponseDto).orElse(null);
    }

    // UPDATE - update existing Feed by ID
    @Override
    public FeedResponseDto update(Long id, FeedRequestDto request, String updatedBy) {
        Feed entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feed", "id", id));

        // update fields from request
        entity.setBatchType(FeedBatchType.valueOf(request.getBatchType().toUpperCase()));
        entity.setFeedName(request.getFeedName());
        entity.setBatchId(request.getBatchId());
        entity.setNotes(request.getNotes());
        entity.setQuantityUsed(request.getQuantityUsed());
        entity.setUpdatedBy(updatedBy);

        repo.save(entity);
        return FeedMapper.toResponseDto(entity);
    }

    // DELETE - delete by ID
    @Override
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Feed", "id", id);
        }
        repo.deleteById(id);
    }

    // SUMMARY
    @Override
    public Map<String, Object> getSummary() {
        List<Feed> all = repo.findAll();
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
        summary.put("quantityByType", quantityByType);

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

        return summary;
    }

}
