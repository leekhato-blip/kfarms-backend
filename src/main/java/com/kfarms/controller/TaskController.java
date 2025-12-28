package com.kfarms.controller;


import com.kfarms.dto.TaskRequestDto;
import com.kfarms.dto.TaskResponseDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.Task;
import com.kfarms.service.impl.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService service;

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<TaskResponseDto>>> upcoming(@RequestParam(value = "limit", defaultValue = "4") int limit) {
        var tasks = service.getUpcoming(limit);
        var response = tasks.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Pending tasks", response)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponseDto>>> allPending() {
        var tasks = service.getAllPending();
        var response = tasks.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Pending Tasks", response)
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponseDto>> create(@RequestBody TaskRequestDto request) {
        Task task = fromReq(request);
        Task saved = service.create(task);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Task created", toDto(saved))
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponseDto>> update(@PathVariable Long id, @RequestBody TaskRequestDto request) {
        Task t = fromReq(request);
        Task updated = service.update(id, t);
        if (updated == null) return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Task not found", null));
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Task updated", toDto(updated))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        boolean ok = service.delete(id);
        if (!ok) return ResponseEntity
                .badRequest()
                .body(
                        new ApiResponse<>(false, "Task not found", null)
                );
        return ResponseEntity.ok(new ApiResponse<>(true, "Task deleted", null));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<TaskResponseDto>> complete(@PathVariable Long id) {
        Task task = service.markComplete(id);
        if (task == null) return ResponseEntity
                .badRequest()
                .body(
                        new ApiResponse<>(false, "Task not found", null)
                );
        return ResponseEntity.ok(new ApiResponse<>(true, "Task completed", toDto(task)));
    }

    /* helper mapping functions */
    private TaskResponseDto toDto(Task t) {
        return TaskResponseDto.builder()
                .id(t.getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .type(t.getType())
                .status(t.getStatus())
                .source(t.getSource())
                .dueDate(t.getDueDate())
                .priority(t.getPriority())
                .relatedEntityType(t.getRelatedEntityType())
                .relatedEntityId(t.getRelatedEntityId())
                .createdAt(t.getCreatedAt())
                .createdBy(t.getCreatedBy())
                .build();
    }

    private Task fromReq(TaskRequestDto r) {
        Task t = new Task();
        t.setTitle(r.getTitle());
        t.setDescription(r.getDescription());
        t.setType(r.getType() == null ? t.getType() : r.getType());
        t.setStatus(r.getStatus() == null ? t.getStatus() : r.getStatus());
        t.setSource(r.getSource() == null ? t.getSource() : r.getSource());
        t.setDueDate(r.getDueDate());
        t.setPriority(r.getPriority() == null ? t.getPriority() : r.getPriority());
        t.setRelatedEntityType(r.getRelatedEntityType());
        t.setRelatedEntityId(r.getRelatedEntityId());
        return t;
    }
}
