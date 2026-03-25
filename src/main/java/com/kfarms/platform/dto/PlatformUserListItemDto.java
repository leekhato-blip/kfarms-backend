package com.kfarms.platform.dto;

import com.kfarms.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PlatformUserListItemDto {

    private Long id;
    private String username;
    private String email;

    private Role role;              // USER / PLATFORM_ADMIN
    private boolean platformAccess;
    private boolean active;
    private int tenantCount;         // number of memberships

    private LocalDateTime createdAt;
}
