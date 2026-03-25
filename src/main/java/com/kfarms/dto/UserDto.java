package com.kfarms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String phoneNumber;
    private boolean emailVerified;
    private boolean phoneVerified;
    private boolean platformAccess;
    private boolean enabled;
}
