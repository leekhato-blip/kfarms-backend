package com.kfarms.service.impl;


import com.kfarms.entity.Task;
import com.kfarms.entity.TaskSource;
import com.kfarms.entity.TaskStatus;
import com.kfarms.repository.TaskRepository;
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

    public Task create(Task t) {
        if (t.getSource() == null) t.setSource(TaskSource.MANUAL);
        if (t.getStatus() == null) t.setStatus(TaskStatus.PENDING);
        return taskRepo.save(t);
    }

    public Task update(Long id, Task payload){
        Optional<Task> opt = taskRepo.findById(id);
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
        if (!taskRepo.existsById(id)) return false;
        taskRepo.deleteById(id);
        return true;
    }

    @Transactional
    public Task markComplete(Long id) {
        var opt = taskRepo.findById(id);
        if (opt.isEmpty()) return null;
        Task t = opt.get();
        t.setStatus(TaskStatus.COMPLETED);
        return taskRepo.save(t);
    }

    public List<Task> getUpcoming(int limit) {
        // fetch pending tasks from new onward
        List<Task> tasks = taskRepo.findByStatusAndDueDateAfterOrderByDueDateAsc(TaskStatus.PENDING, LocalDateTime.now().minusDays(1));
        if (limit > 0 && tasks.size() > limit) return tasks.subList(0, limit);
        return tasks;
    }

    public List<Task> getAllPending() {
        return taskRepo.findByStatusOrderByDueDateAsc(TaskStatus.PENDING);
    }
}
