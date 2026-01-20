package com.kfarms.service.impl;

import com.kfarms.entity.SoftDeletable;
import com.kfarms.service.CleanupService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CleanupServiceImpl implements CleanupService {

    @Transactional
    @Override
    public <T extends SoftDeletable> int deleteSoftDeletedOlderThan(
            Class<T> entityClass, LocalDateTime threshold) {

        // We need to get the repository for the entity
        // For simplicity, inject each repository that needs cleanup and call deleteSoftDeletedOlderThan
        // e.g., salesRepo.deleteSoftDeletedOlderThan(threshold);
        throw new UnsupportedOperationException(
                "Inject the specific repository and call deleteSoftDeletedOlderThan directly"
        );
    }
}
