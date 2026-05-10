package com.kfarms.dto;

import com.kfarms.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class NotificationResponseDto {
    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private boolean read;
    private boolean personal;
    private LocalDateTime createdAt;
}
