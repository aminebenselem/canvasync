package com.whiteboard.backend.collaboration;

import com.whiteboard.backend.board.access.BoardAccessPolicyCachingService;
import com.whiteboard.backend.board.exception.BoardAccessDeniedException;
import com.whiteboard.backend.collaboration.config.WebSocketAuthentication;
import com.whiteboard.backend.collaboration.events.element.CreateElementEvent;
import com.whiteboard.backend.element.ElementService;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;
@Controller
public class CollaborationElementController {

    private final ElementService elementService;
    private final BoardAccessPolicyCachingService cachingService;

    public CollaborationElementController(
            ElementService elementService,
            BoardAccessPolicyCachingService cachingService
    ) {
        this.elementService = elementService;
        this.cachingService = cachingService;
    }

    @MessageMapping("/boards/{boardId}/elements/create")
    public void createElement(
            @DestinationVariable UUID boardId,
            CreateElementEvent event,
            Principal principal
    ) {

        Long userId =
                ((WebSocketAuthentication) principal)
                        .getUserId();

        if (!cachingService.canEdit(boardId, userId)) {
            throw new BoardAccessDeniedException(
                    "User cannot modify this board"
            );
        }

        if ("SHAPE".equals(event.elementType())) {

            if (event.shape() == null) {
                throw new IllegalArgumentException(
                        "Shape payload is required"
                );
            }

            elementService.createShapeElement(
                    boardId,
                    userId,
                    event.shape()
            );

        } else if ("STROKE".equals(event.elementType())) {

            if (event.stroke() == null) {
                throw new IllegalArgumentException(
                        "Stroke payload is required"
                );
            }

            elementService.createStrokeElement(
                    boardId,
                    userId,
                    event.stroke()
            );

        } else {

            throw new IllegalArgumentException(
                    "Unknown element type: " +
                            event.elementType()
            );
        }
    }
}