package com.whiteboard.backend.collaboration;

import com.whiteboard.backend.board.access.BoardAccessPolicyCachingService;
import com.whiteboard.backend.board.exception.UnauthorizedUserException;
import com.whiteboard.backend.collaboration.config.WebSocketAuthentication;
import com.whiteboard.backend.collaboration.events.cursor.CursorEvent;
import com.whiteboard.backend.collaboration.events.cursor.CursorMovedEvent;
import com.whiteboard.backend.collaboration.redis.BoardEventPublisher;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
public class CollaborationController {
    private final BoardAccessPolicyCachingService cachingService;
    private final BoardEventPublisher eventPublisher;
    public CollaborationController(
            BoardAccessPolicyCachingService cachingService, BoardEventPublisher eventPublisher
    ) {
        this.cachingService = cachingService;
        this.eventPublisher = eventPublisher;
    }

    @MessageMapping("/boards/{boardId}/cursor")
    public void handleCursor(
            @DestinationVariable UUID boardId,
            CursorEvent event,
            Principal principal
    ) {

        Long userId =
                ((WebSocketAuthentication) principal)
                        .getUserId();

        if (!cachingService.canView(boardId, userId)) {
            throw new IllegalArgumentException(
                    "User is not allowed to send cursor events"
            );
        }
String username = ((WebSocketAuthentication) principal).getJwt().getClaimAsString("username");
        CursorMovedEvent outgoingEvent =
                new CursorMovedEvent(
                        userId,
                        username,
                        event.x(),
                        event.y()
                );

        eventPublisher.publish(
                boardId,
                "CURSOR_MOVED",
                outgoingEvent
        );
    }
}