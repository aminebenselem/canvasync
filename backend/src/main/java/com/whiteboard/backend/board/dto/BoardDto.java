package com.whiteboard.backend.board.dto;

import java.time.Instant;
import java.util.UUID;

public record BoardDto(
        UUID id,
        String name,
        Long ownerId,
        Instant createdAt,
        Instant updatedAt
) {}