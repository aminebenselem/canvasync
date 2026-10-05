package com.whiteboard.backend.collaboration;

import com.whiteboard.backend.board.access.BoardAccessPolicyCachingService;
import com.whiteboard.backend.board.exception.BoardAccessDeniedException;
import com.whiteboard.backend.collaboration.config.WebSocketAuthentication;
import com.whiteboard.backend.collaboration.events.element.CreateElementEvent;
import com.whiteboard.backend.element.ElementService;
import com.whiteboard.backend.element.dto.CreateShapeDto;
import com.whiteboard.backend.element.dto.CreateStrokeDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CollaborationElementControllerTest {

    private ElementService elementService;
    private BoardAccessPolicyCachingService cachingService;
    private CollaborationElementController controller;

    private WebSocketAuthentication authentication;

    private UUID boardId;
    private Long userId;

    @BeforeEach
    void setUp() {

        elementService =
                mock(ElementService.class);

        cachingService =
                mock(BoardAccessPolicyCachingService.class);

        controller =
                new CollaborationElementController(
                        elementService,
                        cachingService
                );

        authentication =
                mock(WebSocketAuthentication.class);

        boardId =
                UUID.randomUUID();

        userId =
                42L;

        when(authentication.getUserId())
                .thenReturn(userId);
    }

    @Test
    void createElement_shouldCreateShape_whenUserCanEdit() {

        when(cachingService.canEdit(
                boardId,
                userId
        )).thenReturn(true);

        CreateElementEvent event =
                mock(CreateElementEvent.class);

        CreateShapeDto shape =
                mock(CreateShapeDto.class);

        when(event.elementType())
                .thenReturn("SHAPE");

        when(event.shape())
                .thenReturn(shape);

        controller.createElement(
                boardId,
                event,
                authentication
        );

        verify(elementService)
                .createShapeElement(
                        boardId,
                        userId,
                        shape
                );

        verify(
                elementService,
                never()
        ).createStrokeElement(
                any(),
                any(),
                any()
        );
    }

    @Test
    void createElement_shouldCreateStroke_whenUserCanEdit() {

        when(cachingService.canEdit(
                boardId,
                userId
        )).thenReturn(true);

        CreateElementEvent event =
                mock(CreateElementEvent.class);

        CreateStrokeDto stroke =
                mock(CreateStrokeDto.class);

        when(event.elementType())
                .thenReturn("STROKE");

        when(event.stroke())
                .thenReturn(stroke);

        controller.createElement(
                boardId,
                event,
                authentication
        );

        verify(elementService)
                .createStrokeElement(
                        boardId,
                        userId,
                        stroke
                );

        verify(
                elementService,
                never()
        ).createShapeElement(
                any(),
                any(),
                any()
        );
    }

    @Test
    void createElement_shouldReject_whenUserCannotEdit() {

        when(cachingService.canEdit(
                boardId,
                userId
        )).thenReturn(false);

        CreateElementEvent event =
                mock(CreateElementEvent.class);

        when(event.elementType())
                .thenReturn("SHAPE");

        when(event.shape())
                .thenReturn(
                        mock(CreateShapeDto.class)
                );

        assertThrows(
                BoardAccessDeniedException.class,
                () -> controller.createElement(
                        boardId,
                        event,
                        authentication
                )
        );

        verifyNoInteractions(elementService);
    }

    @Test
    void createElement_shouldRejectShapeWithoutPayload() {

        when(cachingService.canEdit(
                boardId,
                userId
        )).thenReturn(true);

        CreateElementEvent event =
                mock(CreateElementEvent.class);

        when(event.elementType())
                .thenReturn("SHAPE");

        when(event.shape())
                .thenReturn(null);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> controller.createElement(
                                boardId,
                                event,
                                authentication
                        )
                );

        assertEquals(
                "Shape payload is required",
                exception.getMessage()
        );

        verifyNoInteractions(elementService);
    }

    @Test
    void createElement_shouldRejectStrokeWithoutPayload() {

        when(cachingService.canEdit(
                boardId,
                userId
        )).thenReturn(true);

        CreateElementEvent event =
                mock(CreateElementEvent.class);

        when(event.elementType())
                .thenReturn("STROKE");

        when(event.stroke())
                .thenReturn(null);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> controller.createElement(
                                boardId,
                                event,
                                authentication
                        )
                );

        assertEquals(
                "Stroke payload is required",
                exception.getMessage()
        );

        verifyNoInteractions(elementService);
    }

    @Test
    void createElement_shouldRejectUnknownElementType() {

        when(cachingService.canEdit(
                boardId,
                userId
        )).thenReturn(true);

        CreateElementEvent event =
                mock(CreateElementEvent.class);

        when(event.elementType())
                .thenReturn("BANANA");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> controller.createElement(
                                boardId,
                                event,
                                authentication
                        )
                );

        assertEquals(
                "Unknown element type: BANANA",
                exception.getMessage()
        );

        verifyNoInteractions(elementService);
    }
}