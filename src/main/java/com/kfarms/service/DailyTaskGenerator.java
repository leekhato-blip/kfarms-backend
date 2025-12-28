package com.kfarms.service;

import com.kfarms.entity.RecurringTaskRule;
import com.kfarms.entity.Task;
import com.kfarms.entity.TaskSource;
import com.kfarms.entity.TaskStatus;
import com.kfarms.repository.RecurringTaskRulesRepo;
import com.kfarms.repository.TaskRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DailyTaskGenerator {

    private final RecurringTaskRulesRepo rulesRepo;
    private final TaskRepository taskRepo;

    @PostConstruct
    public void initToday() {
        createTasksFor(LocalDate.now());
    }

    @Scheduled(cron = "0 0 0 * * *") // runs at midnight
    public void generateDailyTasks() {
        createTasksFor(LocalDate.now());
    }
    private void createTasksFor(LocalDate date) {
        List<RecurringTaskRule> rules = rulesRepo.findByEnabledTrue();

        for (RecurringTaskRule rule : rules) {

            // SAFETY CHECK
            if (rule.getTimes() == null || rule.getTimes().isEmpty()) {
                continue;
            }

            for (LocalTime time : rule.getTimes()) {

                LocalDateTime due = date.atTime(time);

                boolean exists = taskRepo.existsByTitleAndDueDateAndSource(
                        rule.getTitle(),
                        due,
                        TaskSource.AUTO
                );

                if (!exists) {
                    taskRepo.save(
                            Task.builder()
                                    .title(rule.getTitle())
                                    .description("Auto-generated from rule")
                                    .type(rule.getTaskType())
                                    .source(TaskSource.AUTO)
                                    .status(TaskStatus.PENDING)
                                    .priority(2)
                                    .dueDate(due)
                                    .relatedEntityType(rule.getEntityType())
                                    .relatedEntityId(rule.getEntityId())
                                    .build()
                    );
                }
            }
        }
    }
}
