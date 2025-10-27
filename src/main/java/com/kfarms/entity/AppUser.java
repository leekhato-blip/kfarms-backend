package com.kfarms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Data
@RequiredArgsConstructor
public class AppUser extends Auditable{
    @Id
    @GeneratedValue
    private Long id;
    @Column(unique = true)
    private String username;
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;
}
