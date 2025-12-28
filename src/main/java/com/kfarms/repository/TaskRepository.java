package com.kfarms.repository;

import com.kfarms.entity.Task;
import com.kfarms.entity.TaskSource;
import com.kfarms.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // upcoming tasks not completed, from given datetime
    List<Task> findByStatusAndDueDateAfterOrderByDueDateAsc(TaskStatus status, LocalDateTime from);

    // all pending tasks ordered by dueDate asc
    List<Task> findByStatusOrderByDueDateAsc(TaskStatus status);

    // Exists by Title, Due date and Source
    boolean existsByTitleAndDueDateAndSource(String title, LocalDateTime dueDate, TaskSource source);

}
