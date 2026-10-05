package com.whiteboard.backend.collaboration.events.cursor;

public record CursorMovedEvent(
        Long userId,
        String username,
        double x,
        double y
) {
}
