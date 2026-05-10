package com.kfarms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notification_read_state",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_read_state_notification_user",
                        columnNames = {"notification_id", "user_id"}
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationReadState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Notification notification;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AppUser user;

    @Builder.Default
    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt = LocalDateTime.now();
}
