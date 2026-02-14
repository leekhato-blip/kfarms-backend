package com.kfarms.repository;

import com.kfarms.entity.Notification;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // ---------- READ QUERIES ----------

    List<Notification> findByReadFalseOrderByCreatedAtDesc();

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    @Query("""
        select n from Notification n
        where (n.user is null or n.user.id = :userId)
          and (n.read = false or n.read is null)
        order by n.createdAt desc
    """)
    List<Notification> findUnreadForUserOrGlobal(@Param("userId") Long userId);

    @Query("""
        select n from Notification n
        where lower(n.title) like lower(concat('%', :q, '%'))
           or lower(n.message) like lower(concat('%', :q, '%'))
        order by n.createdAt desc
    """)
    List<Notification> searchByTitleOrMessage(@Param("q") String q, Pageable pageable);

    boolean existsByTitleAndMessageAndCreatedAtAfter(
            String title,
            String message,
            LocalDateTime createdAt
    );

    @Query("""
    select n from Notification n
    where (n.user is null or n.user.id = :userId)
    order by n.createdAt desc
    """)
    List<Notification> findForUserOrGlobal(@Param("userId") Long userId);


    // ---------- DELETE (CLEANUP) ----------

    @Modifying
    @Transactional
    int deleteByCreatedAtBefore(LocalDateTime cutoff);

    @Modifying
    @Transactional
    int deleteByReadTrueAndCreatedAtBefore(LocalDateTime cutoff);

    // ✅ MISSING METHOD (THIS FIXES YOUR ERROR)
    @Modifying
    @Transactional
    int deleteByReadFalseAndCreatedAtBefore(LocalDateTime cutoff);
}
