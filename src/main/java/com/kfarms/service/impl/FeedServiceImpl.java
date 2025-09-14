package com.kfarms.service.impl;

import com.kfarms.dto.FeedRequestDto;
import com.kfarms.dto.FeedResponseDto;
import com.kfarms.entity.Feed;
import com.kfarms.entity.FeedBatchType;
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

import java.util.*;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {
    private final FeedRepository repo;

    // READ - get all with filtering & pagination
    @Override
    public Map<String, Object> getAll(int page, int size, String batchType) {
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

    // CREATE - add new feed
    @Override
    public FeedResponseDto save(FeedRequestDto dto) {
        Feed entity = FeedMapper.toEntity(dto);
        Feed saved = repo.save(entity);
        return FeedMapper.toResponseDto(saved);
    }

    // DELETE - delete by ID
    @Override
    public void delete(Long id) { repo.deleteById(id); }
}
