package com.kfarms.dto;

import com.kfarms.entity.TaskSource;
import com.kfarms.entity.TaskStatus;
import com.kfarms.entity.TaskType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskResponseDto {
    private Long id;
    private String title;
    private String description;
    private TaskType type;
    private TaskStatus status;
    private TaskSource source;
    private LocalDateTime dueDate;
    private Integer priority;
    private String relatedEntityType;
    private Long relatedEntityId;
    private String createdBy;
    private LocalDateTime createdAt;
}
