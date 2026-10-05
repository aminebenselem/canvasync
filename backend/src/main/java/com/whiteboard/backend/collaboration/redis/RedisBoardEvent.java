package com.whiteboard.backend.collaboration.redis;

import java.util.UUID;

public record RedisBoardEvent(
        String type,
        UUID boardId,
        Object payload
) {
}