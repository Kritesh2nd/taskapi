package com.techaxis.taskapi.dto;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        Instant createdAt
) {}
