package com.kfarms.repository;

import com.kfarms.entity.Notification;
import com.kfarms.entity.NotificationType;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // ---------- READ QUERIES ----------

    List<Notification> findByTenant_IdAndReadFalseOrderByCreatedAtDesc(Long tenantId);

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    List<Notification> findAllByTenant_IdOrderByCreatedAtDesc(Long tenantId);

    Optional<Notification> findByIdAndTenant_Id(Long id, Long tenantId);

    List<Notification> findByIdInAndTenant_Id(List<Long> ids, Long tenantId);

    @Query("""
        select n from Notification n
        where n.tenant.id = :tenantId
          and (n.user is null or n.user.id = :userId)
          and (n.read = false or n.read is null)
        order by n.createdAt desc
    """)
    List<Notification> findUnreadForUserOrGlobal(
            @Param("tenantId") Long tenantId,
            @Param("userId") Long userId
    );

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
        select (count(n) > 0) from Notification n
        where n.tenant.id = :tenantId
          and n.type = :type
          and lower(n.title) = lower(:title)
          and lower(n.message) = lower(:message)
          and n.createdAt >= :since
          and n.user is null
    """)
    boolean existsRecentGlobalDuplicate(
            @Param("tenantId") Long tenantId,
            @Param("type") NotificationType type,
            @Param("title") String title,
            @Param("message") String message,
            @Param("since") LocalDateTime since
    );

    @Query("""
        select (count(n) > 0) from Notification n
        where n.tenant.id = :tenantId
          and n.type = :type
          and lower(n.title) = lower(:title)
          and lower(n.message) = lower(:message)
          and n.createdAt >= :since
          and n.user.id = :userId
    """)
    boolean existsRecentUserDuplicate(
            @Param("tenantId") Long tenantId,
            @Param("userId") Long userId,
            @Param("type") NotificationType type,
            @Param("title") String title,
            @Param("message") String message,
            @Param("since") LocalDateTime since
    );

    @Query("""
    select n from Notification n
    where n.tenant.id = :tenantId
      and (n.user is null or n.user.id = :userId)
    order by n.createdAt desc
    """)
    List<Notification> findForUserOrGlobal(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @Query("""
    select n from Notification n
    where n.tenant.id = :tenantId
      and (n.read = false or n.read is null)
    order by n.createdAt desc
    """)
    List<Notification> findUnreadByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);


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

    long countByTenant_Id(Long tenantId);

}
