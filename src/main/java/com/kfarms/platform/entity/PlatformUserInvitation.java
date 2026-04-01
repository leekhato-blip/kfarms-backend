package com.kfarms.platform.entity;

import com.kfarms.entity.Auditable;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Table(name = "platform_user_invitations", indexes = {
        @Index(name = "idx_platform_invite_token", columnList = "token", unique = true),
        @Index(name = "idx_platform_invite_email", columnList = "email"),
        @Index(name = "idx_platform_invite_username", columnList = "username")
})
@Where(clause = "deleted = false")
public class PlatformUserInvitation extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, unique = true, length = 80)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean accepted = false;
}
