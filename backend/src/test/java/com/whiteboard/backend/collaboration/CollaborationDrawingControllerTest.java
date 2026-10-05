package com.whiteboard.backend.collaboration;

import com.whiteboard.backend.board.access.BoardAccessPolicyCachingService;
import com.whiteboard.backend.board.exception.BoardAccessDeniedException;
import com.whiteboard.backend.collaboration.config.WebSocketAuthentication;
import com.whiteboard.backend.collaboration.events.drawing.DrawingEvent;
import com.whiteboard.backend.collaboration.redis.BoardEventPublisher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CollaborationDrawingControllerTest {

    private BoardAccessPolicyCachingService cachingService;
    private BoardEventPublisher eventPublisher;
    private CollaborationDrawingController controller;

    private WebSocketAuthentication authentication;

    private final UUID boardId =
            UUID.randomUUID();

    private final Long userId =
            42L;

    @BeforeEach
    void setUp() {

        cachingService =
                mock(BoardAccessPolicyCachingService.class);

        eventPublisher =
                mock(BoardEventPublisher.class);

        controller =
                new CollaborationDrawingController(
                        eventPublisher,
                        cachingService
                );

        authentication =
                mock(WebSocketAuthentication.class);

        when(authentication.getUserId())
                .thenReturn(userId);
    }

    @Test
    void handleDrawing_shouldPublish_whenUserCanEdit() {

        when(cachingService.canEdit(
                boardId,
                userId
        )).thenReturn(true);

        DrawingEvent.PointDto point =
                new DrawingEvent.PointDto(
                        10,
                        20
                );

        DrawingEvent.PointDto startPoint =
                new DrawingEvent.PointDto(
                        10,
                        20
                );

        DrawingEvent.PointDto endPoint =
                new DrawingEvent.PointDto(
                        100,
                        200
                );

        DrawingEvent event =
                new DrawingEvent(
                        "UPDATE",
                        "SHAPE",
                        "element-123",
                        "MOVE",
                        "rectangle",
                        point,
                        startPoint,
                        endPoint,
                        "#000000",
                        2.0,
                        List.of(point)
                );

        controller.handleDrawing(
                boardId,
                event,
                authentication
        );

        ArgumentCaptor<CollaborationDrawingController.DrawingBroadcast>
                captor =
                ArgumentCaptor.forClass(
                        CollaborationDrawingController.DrawingBroadcast.class
                );

        verify(eventPublisher).publish(
                eq(boardId),
                eq("DRAWING"),
                captor.capture()
        );

        CollaborationDrawingController.DrawingBroadcast
                published =
                captor.getValue();

        assertEquals(
                userId,
                published.userId()
        );

        DrawingEvent publishedEvent =
                published.event();

        assertEquals(
                "UPDATE",
                publishedEvent.action()
        );

        assertEquals(
                "SHAPE",
                publishedEvent.elementType()
        );

        assertEquals(
                "element-123",
                publishedEvent.elementId()
        );

        assertEquals(
                "MOVE",
                publishedEvent.operation()
        );

        assertEquals(
                "rectangle",
                publishedEvent.shapeType()
        );

        assertEquals(
                startPoint,
                publishedEvent.startPoint()
        );

        assertEquals(
                endPoint,
                publishedEvent.endPoint()
        );

        assertEquals(
                "#000000",
                publishedEvent.color()
        );

        assertEquals(
                2.0,
                publishedEvent.width()
        );
    }

    @Test
    void handleDrawing_shouldReject_whenUserCannotEdit() {

        when(cachingService.canEdit(
                boardId,
                userId
        )).thenReturn(false);

        DrawingEvent event =
                new DrawingEvent(
                        "START",
                        "STROKE",
                        null,
                        "DRAW",
                        null,
                        new DrawingEvent.PointDto(
                                10,
                                20
                        ),
                        null,
                        null,
                        "#000000",
                        2.0,
                        null
                );

        assertThrows(
                BoardAccessDeniedException.class,
                () -> controller.handleDrawing(
                        boardId,
                        event,
                        authentication
                )
        );

        verify(
                eventPublisher,
                never()
        ).publish(
                any(),
                any(),
                any()
        );
    }

    @Test
    void handleDrawing_shouldPreserveEventData() {

        when(cachingService.canEdit(
                boardId,
                userId
        )).thenReturn(true);

        DrawingEvent event =
                new DrawingEvent(
                        "END",
                        "STROKE",
                        "element-999",
                        "DRAW",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        controller.handleDrawing(
                boardId,
                event,
                authentication
        );

        ArgumentCaptor<CollaborationDrawingController.DrawingBroadcast>
                captor =
                ArgumentCaptor.forClass(
                        CollaborationDrawingController.DrawingBroadcast.class
                );

        verify(eventPublisher).publish(
                eq(boardId),
                eq("DRAWING"),
                captor.capture()
        );

        DrawingEvent published =
                captor.getValue().event();

        assertEquals(
                event.action(),
                published.action()
        );

        assertEquals(
                event.elementType(),
                published.elementType()
        );

        assertEquals(
                event.elementId(),
                published.elementId()
        );

        assertEquals(
                event.operation(),
                published.operation()
        );
    }
}