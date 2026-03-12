package com.kfarms.service.impl;


import com.kfarms.entity.Task;
import com.kfarms.entity.TaskSource;
import com.kfarms.entity.TaskStatus;
import com.kfarms.repository.TaskRepository;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.service.TenantContext;
import com.kfarms.tenant.service.TenantPlanGuardService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class TaskService {
    private final TaskRepository taskRepo;
    private final TenantPlanGuardService planGuardService;

    public Task create(Task t) {
        Tenant tenant = planGuardService.requireCurrentTenant();
        if (t.getSource() == null) t.setSource(TaskSource.MANUAL);
        if (t.getStatus() == null) t.setStatus(TaskStatus.PENDING);
        t.setTenant(tenant);
        return taskRepo.save(t);
    }

    public Task update(Long id, Task payload){
        Long tenantId = TenantContext.getTenantId();
        Optional<Task> opt = taskRepo.findByIdAndTenant_Id(id, tenantId);
        if (opt.isEmpty()) return null;
        Task exist = opt.get();
        exist.setTitle(payload.getTitle());
        exist.setDescription(payload.getDescription());
        exist.setType(payload.getType());
        exist.setDueDate(payload.getDueDate());
        exist.setPriority(payload.getPriority());
        exist.setRelatedEntityType(payload.getRelatedEntityType());
        exist.setRelatedEntityId(payload.getRelatedEntityId());
        exist.setStatus(payload.getStatus());
        exist.setSource(payload.getSource());
        return taskRepo.save(exist);
    }

    public boolean delete(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Optional<Task> opt = taskRepo.findByIdAndTenant_Id(id, tenantId);
        if (opt.isEmpty()) return false;
        taskRepo.delete(opt.get());
        return true;
    }

    @Transactional
    public Task markComplete(Long id) {
        Long tenantId = TenantContext.getTenantId();
        var opt = taskRepo.findByIdAndTenant_Id(id, tenantId);
        if (opt.isEmpty()) return null;
        Task t = opt.get();
        t.setStatus(TaskStatus.COMPLETED);
        return taskRepo.save(t);
    }

    public List<Task> getUpcoming(int limit) {
        Long tenantId = TenantContext.getTenantId();
        List<Task> tasks = taskRepo.findActiveByTenantIdAndStatus(tenantId, TaskStatus.PENDING).stream()
                .filter(task -> task.getDueDate() == null || task.getDueDate().isAfter(LocalDateTime.now().minusDays(1)))
                .toList();
        if (limit > 0 && tasks.size() > limit) return tasks.subList(0, limit);
        return tasks;
    }

    public List<Task> getAllPending() {
        Long tenantId = TenantContext.getTenantId();
        return taskRepo.findActiveByTenantIdAndStatus(tenantId, TaskStatus.PENDING);
    }
}
