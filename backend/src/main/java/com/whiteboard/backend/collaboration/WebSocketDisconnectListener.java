package com.whiteboard.backend.collaboration;

import com.whiteboard.backend.board.access.BoardAccessPolicyCachingService;
import com.whiteboard.backend.collaboration.events.cursor.CursorLeftEvent;
import com.whiteboard.backend.collaboration.config.WebSocketAuthentication;
import com.whiteboard.backend.collaboration.redis.BoardEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.UUID;

@Component
public class WebSocketDisconnectListener {

    private final BoardAccessPolicyCachingService cachingService;
    private final BoardEventPublisher eventPublisher;
    public WebSocketDisconnectListener(
            BoardAccessPolicyCachingService cachingService, BoardEventPublisher eventPublisher
    ) {
        this.cachingService = cachingService;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    public void handleDisconnect(
            SessionDisconnectEvent event
    ) {

        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(
                        event.getMessage()
                );

        Object boardIdValue =
                accessor.getSessionAttributes()
                        .get("boardId");

        if (boardIdValue == null) {
            return;
        }

        UUID boardId =
                UUID.fromString(
                        boardIdValue.toString()
                );

        if (!(accessor.getUser()
                instanceof WebSocketAuthentication authentication)) {
            return;
        }

        Long userId =
                authentication.getUserId();

        // Remove temporary permission cache
        cachingService.removePermission(
                boardId,
                userId
        );

        eventPublisher.publish(
                boardId,
                "CURSOR_LEFT",
                new CursorLeftEvent(userId)
        );
    }
}