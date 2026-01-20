package com.kfarms.config;

import com.kfarms.repository.SalesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SoftDeleteCleanupJob {

    private final SalesRepository salesRepo;

    @Transactional // ensures the modifying query runs inside a transaction
    @Scheduled(cron = "0 0 3 * * ?") // Every day at 3am
    public void cleanupOldTrash() {
        LocalDateTime threshold = LocalDateTime.now().minusMonths(1);

        int salesDeleted = salesRepo.deleteSoftDeletedOlderThan(threshold);
        log.info("Cleanup completed. Sales deleted: {}", salesDeleted);
    }
}
