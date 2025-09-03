package com.kfarms.service.impl;

import com.kfarms.dto.FeedRequestDto;
import com.kfarms.dto.FeedResponseDto;
import com.kfarms.entity.Feed;
import com.kfarms.mapper.FeedMapper;
import com.kfarms.repository.FeedRepository;
import com.kfarms.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {
    private final FeedRepository repo;

    @Override
    public Page<FeedResponseDto> getAll(String batchType, String feedName, Pageable pageable) {
        Specification<Feed> spec = (root, query, cb) -> cb.conjunction();

        if (batchType != null && !batchType.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("batchType"), batchType));
        }
        if (feedName != null && !feedName.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("feedName")), "%" + feedName.toLowerCase() + "%")
            );
        }
        return repo.findAll(spec, pageable).map(FeedMapper::toResponseDto);
    }
    @Override
    public FeedResponseDto getById(Long id) {
        Optional<Feed> feed = repo.findById(id);
        return feed.map(FeedMapper::toResponseDto).orElse(null);
    }

    @Override
    public FeedResponseDto save(FeedRequestDto dto) {
        Feed entity = FeedMapper.toEntity(dto);
        Feed saved = repo.save(entity);
        return FeedMapper.toResponseDto(saved);
    }

    @Override
    public void delete(Long id) { repo.deleteById(id); }
}
