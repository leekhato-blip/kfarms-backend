package com.kfarms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
public class AppUser extends Auditable {
    @Id
    @GeneratedValue private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Username is required")
    private String username;

    @Column(nullable = false, unique = true)
    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @Column(name = "phone_number", unique = true, length = 24)
    private String phoneNumber;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "platform_access", nullable = false)
    private boolean platformAccess = false;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "email_verified")
    private Boolean emailVerified = true;

    @Column(name = "phone_verified")
    private Boolean phoneVerified = true;

    @Column(name = "email_verification_code", length = 12)
    private String emailVerificationCode;

    @Column(name = "phone_verification_code", length = 12)
    private String phoneVerificationCode;

    @Column(name = "email_verification_expires_at")
    private java.time.LocalDateTime emailVerificationExpiresAt;

    @Column(name = "phone_verification_expires_at")
    private java.time.LocalDateTime phoneVerificationExpiresAt;
}
