package com.techaxis.taskapi.controller;

import com.techaxis.taskapi.dto.TaskRequest;
import com.techaxis.taskapi.dto.TaskResponse;
import com.techaxis.taskapi.entity.TaskStatus;
import com.techaxis.taskapi.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        TaskResponse created = taskService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/tasks/" + created.id())).body(created);
    }

    @GetMapping
    public Page<TaskResponse> findAll(
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) TaskStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return taskService.findAll(ownerId, status, pageable);
    }

    @GetMapping("/{id}")
    public TaskResponse findById(@PathVariable Long id) {
        return taskService.findById(id);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return taskService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        taskService.delete(id);
    }
}
