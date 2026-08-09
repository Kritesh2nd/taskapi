package com.techaxis.taskapi.service;

import com.techaxis.taskapi.dto.TaskRequest;
import com.techaxis.taskapi.dto.TaskResponse;
import com.techaxis.taskapi.entity.Task;
import com.techaxis.taskapi.entity.TaskStatus;
import com.techaxis.taskapi.entity.User;
import com.techaxis.taskapi.exception.ResourceNotFoundException;
import com.techaxis.taskapi.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserService userService;

    public TaskResponse create(TaskRequest request) {
        User owner = userService.getOrThrow(request.ownerId());

        Task task = Task.builder()
                .title(request.title())
                .description(request.description())
                .status(request.status() == null ? TaskStatus.TODO : request.status())
                .dueDate(request.dueDate())
                .owner(owner)
                .build();

        return toResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> findAll(Long ownerId, TaskStatus status, Pageable pageable) {
        Page<Task> page;
        if (ownerId != null && status != null) {
            page = taskRepository.findByOwnerIdAndStatus(ownerId, status, pageable);
        } else if (ownerId != null) {
            page = taskRepository.findByOwnerId(ownerId, pageable);
        } else if (status != null) {
            page = taskRepository.findByStatus(status, pageable);
        } else {
            page = taskRepository.findAll(pageable);
        }
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    public TaskResponse update(Long id, TaskRequest request) {
        Task task = getOrThrow(id);

        if (!task.getOwner().getId().equals(request.ownerId())) {
            User newOwner = userService.getOrThrow(request.ownerId());
            task.setOwner(newOwner);
        }

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status() == null ? task.getStatus() : request.status());
        task.setDueDate(request.dueDate());

        return toResponse(task);
    }

    public void delete(Long id) {
        Task task = getOrThrow(id);
        taskRepository.delete(task);
    }

    private Task getOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getDueDate(),
                task.getOwner().getId(),
                task.getOwner().getName(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
