package com.techaxis.taskapi.repository;

import com.techaxis.taskapi.entity.Task;
import com.techaxis.taskapi.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findByOwnerId(Long ownerId, Pageable pageable);
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);
    Page<Task> findByOwnerIdAndStatus(Long ownerId, TaskStatus status, Pageable pageable);
}
