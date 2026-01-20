package com.kfarms.service;

import com.kfarms.entity.SoftDeletable;

import java.time.LocalDateTime;

public interface CleanupService {
    <T extends SoftDeletable> int deleteSoftDeletedOlderThan(
            Class<T> entityClass,
            LocalDateTime threshold
    );
}
