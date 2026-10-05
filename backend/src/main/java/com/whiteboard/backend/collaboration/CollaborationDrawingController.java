package com.whiteboard.backend.collaboration;

import com.whiteboard.backend.board.access.BoardAccessPolicyCachingService;
import com.whiteboard.backend.board.exception.BoardAccessDeniedException;
import com.whiteboard.backend.collaboration.config.WebSocketAuthentication;
import com.whiteboard.backend.collaboration.events.drawing.DrawingEvent;
import com.whiteboard.backend.collaboration.redis.BoardEventPublisher;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
public class CollaborationDrawingController {

    private final BoardEventPublisher eventPublisher;
    private final BoardAccessPolicyCachingService cachingService;

    public CollaborationDrawingController(
            BoardEventPublisher eventPublisher,
            BoardAccessPolicyCachingService cachingService
    ) {
        this.eventPublisher = eventPublisher;
        this.cachingService = cachingService;
    }

    @MessageMapping("/boards/{boardId}/drawing")
    public void handleDrawing(
            @DestinationVariable UUID boardId,
            DrawingEvent event,
            Principal principal
    ) {

        WebSocketAuthentication authentication =
                (WebSocketAuthentication) principal;

        Long userId = authentication.getUserId();

        if (!cachingService.canEdit(boardId, userId)) {
            throw new BoardAccessDeniedException(
                    "User cannot modify this board"
            );
        }

        DrawingEvent enrichedEvent = new DrawingEvent(
                event.action(),
                event.elementType(),
                event.elementId(),
                event.operation(),
                event.shapeType(),
                event.point(),
                event.startPoint(),
                event.endPoint(),
                event.color(),
                event.width(),
                event.points()
        );
        eventPublisher.publish(
                boardId,
                "DRAWING",
                new DrawingBroadcast(
                        userId,
                        enrichedEvent
                )
        );
    }

    public record DrawingBroadcast(
            Long userId,
            DrawingEvent event
    ) {}
}