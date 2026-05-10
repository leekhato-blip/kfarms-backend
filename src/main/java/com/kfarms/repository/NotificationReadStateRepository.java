package com.kfarms.repository;

import com.kfarms.entity.NotificationReadState;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface NotificationReadStateRepository extends JpaRepository<NotificationReadState, Long> {

    boolean existsByNotification_IdAndUser_Id(Long notificationId, Long userId);

    @Query("""
        select state.notification.id from NotificationReadState state
        where state.user.id = :userId
          and state.notification.id in :notificationIds
    """)
    List<Long> findReadNotificationIds(
            @Param("userId") Long userId,
            @Param("notificationIds") Collection<Long> notificationIds
    );

    @Modifying
    @Transactional
    int deleteByReadAtBefore(LocalDateTime cutoff);
}
