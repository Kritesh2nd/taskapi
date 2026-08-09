package com.techaxis.taskapi.dto;

import com.techaxis.taskapi.entity.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        LocalDate dueDate,
        Long ownerId,
        String ownerName,
        Instant createdAt,
        Instant updatedAt
) {}
