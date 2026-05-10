package com.kfarms.config;

import com.kfarms.repository.FishPondRepository;
import com.kfarms.repository.SalesRepository;
import com.kfarms.repository.SuppliesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Component
@RequiredArgsConstructor
@Slf4j
public class SoftDeleteCleanupJob {

    private final SalesRepository salesRepo;
    private final SuppliesRepository suppliesRepo;
    private final FishPondRepository fishPondRepo;

    @Transactional
    @Scheduled(cron = "0 0 3 * * ?") // Every day at 3am
    public void cleanupOldTrash() {
        LocalDateTime threshold = LocalDateTime.now().minusMonths(1);

        int salesDeleted = salesRepo.cleanupOldSoftDeleted(threshold);
        int suppliesDeleted = suppliesRepo.cleanupOldSoftDeleted(threshold);
        int fishPondDeleted = fishPondRepo.cleanupOldSoftDeleted(threshold);


        log.info("Cleanup completed. Sales deleted: {}", salesDeleted);
        log.info("Cleanup completed. Supplies deleted: {}", suppliesDeleted);
        log.info("Cleanup completed. FishPond deleted: {}", fishPondDeleted);
    }
}

