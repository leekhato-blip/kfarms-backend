package com.kfarms.config;

import com.kfarms.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationCleanupJob {

    private final NotificationRepository repo;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        log.info("Running notification cleanup for items older than 14-30 days");
        cleanupOldNotifications();
    }

    // runs every day at 2:30 AM
    @Scheduled(cron = "0 30 2 * * *")
    public void cleanupOldNotifications() {
        LocalDateTime cutoffRead = LocalDateTime.now().minusDays(30);
        LocalDateTime cutoffUnread = LocalDateTime.now().minusDays(14);

        // Delete READ older than 30 days
        repo.deleteByReadTrueAndCreatedAtBefore(cutoffRead);

        // Delete UNREAD older than 14 days
        repo.deleteByReadFalseAndCreatedAtBefore(cutoffUnread);
    }
}
