package com.techaxis.taskapi.dto;

import com.techaxis.taskapi.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskRequest(
        @NotBlank(message = "title is required")
        @Size(max = 150, message = "title must be at most 150 characters")
        String title,

        @Size(max = 1000, message = "description must be at most 1000 characters")
        String description,

        TaskStatus status,

        LocalDate dueDate,

        @NotNull(message = "ownerId is required")
        Long ownerId
) {}
