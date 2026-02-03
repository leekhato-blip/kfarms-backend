package com.kfarms.repository;

import com.kfarms.entity.AppUser;
import com.kfarms.entity.Notification;
import com.kfarms.entity.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Fetch all unread notifications (any user)
    List<Notification> findByReadFalseOrderByCreatedAtDesc();

    // Fetch only unread notification for a specific user
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    //  Fetch both global and user-specific notifications
    @Query("SELECT n FROM Notification n " +
            "WHERE n.user IS NULL OR n.user.id = :userId " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findForUserOrGlobal(@Param("userId") Long userId);

    @Query("""
    select n from Notification n
    where lower(n.title) like lower(concat('%', :q, '%'))
    or lower(n.message) like lower(concat('%', :q, '%'))
        order by n.createdAt desc
    """)
    List<Notification> searchByTitleOrMessage(@Param("q") String q, Pageable pageable);

}
