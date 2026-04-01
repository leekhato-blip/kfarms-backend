package com.kfarms.platform.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PlatformUserInviteDto {
    private String username;
    private String email;
    private String token;
    private LocalDateTime expiresAt;
}
