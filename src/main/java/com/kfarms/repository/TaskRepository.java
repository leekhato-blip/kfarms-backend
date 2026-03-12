package com.kfarms.repository;

import com.kfarms.entity.Task;
import com.kfarms.entity.TaskSource;
import com.kfarms.entity.TaskStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // upcoming tasks not completed, from given datetime
    List<Task> findByStatusAndDueDateAfterOrderByDueDateAsc(TaskStatus status, LocalDateTime from);

    // all pending tasks ordered by dueDate asc
    List<Task> findByStatusOrderByDueDateAsc(TaskStatus status);

    // Exists by Title, Due date and Source
    boolean existsByTitleAndDueDateAndSource(String title, LocalDateTime dueDate, TaskSource source);

    @Query("""
    select t from Task t
    where lower(t.title) like lower(concat('%', :q, '%'))
        order by t.createdAt desc
    """)
    List<Task> searchByTitle(@Param("q") String q, Pageable pageable);

//    long countByTenantId(Long tenantId);

    @Query("""
    select t from Task t
    where t.tenant.id = :tenantId
      and t.status = :status
      and (t.deleted = false or t.deleted is null)
    order by
      case when t.dueDate is null then 1 else 0 end,
      t.dueDate asc,
      t.id desc
    """)
    List<Task> findActiveByTenantIdAndStatus(
            @Param("tenantId") Long tenantId,
            @Param("status") TaskStatus status
    );

    @Query("""
    select t from Task t
    where t.tenant.id = :tenantId
      and (t.deleted = false or t.deleted is null)
    order by t.createdAt desc
    """)
    List<Task> findRecentActiveByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    Optional<Task> findByIdAndTenant_Id(Long id, Long tenantId);


}
