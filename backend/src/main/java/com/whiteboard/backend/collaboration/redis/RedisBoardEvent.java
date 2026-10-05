package com.whiteboard.backend.collaboration.events.redis;

import java.util.UUID;

public record RedisBoardEvent(
        String type,
        UUID boardId,
        Object payload
) {
}