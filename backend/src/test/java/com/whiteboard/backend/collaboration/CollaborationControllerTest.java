package com.whiteboard.backend.collaboration;

import com.whiteboard.backend.board.access.BoardAccessPolicyCachingService;
import com.whiteboard.backend.collaboration.config.WebSocketAuthentication;
import com.whiteboard.backend.collaboration.events.cursor.CursorEvent;
import com.whiteboard.backend.collaboration.events.cursor.CursorMovedEvent;
import com.whiteboard.backend.collaboration.redis.BoardEventPublisher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CollaborationControllerTest {

    private BoardAccessPolicyCachingService cachingService;
    private BoardEventPublisher eventPublisher;
    private CollaborationController controller;

    private WebSocketAuthentication authentication;
    private Jwt jwt;

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
                new CollaborationController(
                        cachingService,
                        eventPublisher
                );

        authentication =
                mock(WebSocketAuthentication.class);

        jwt =
                mock(Jwt.class);

        when(authentication.getUserId())
                .thenReturn(userId);

        when(authentication.getJwt())
                .thenReturn(jwt);

        when(jwt.getClaimAsString("username"))
                .thenReturn("amine");
    }

    @Test
    void handleCursor_shouldPublishCursorEvent_whenUserCanView() {

        when(cachingService.canView(
                boardId,
                userId
        )).thenReturn(true);

        CursorEvent event =
                new CursorEvent(
                        123.5,
                        456.75
                );

        controller.handleCursor(
                boardId,
                event,
                authentication
        );

        ArgumentCaptor<CursorMovedEvent> captor =
                ArgumentCaptor.forClass(
                        CursorMovedEvent.class
                );

        verify(eventPublisher).publish(
                eq(boardId),
                eq("CURSOR_MOVED"),
                captor.capture()
        );

        CursorMovedEvent published =
                captor.getValue();

        assertEquals(
                userId,
                published.userId()
        );

        assertEquals(
                "amine",
                published.username()
        );

        assertEquals(
                123.5,
                published.x()
        );

        assertEquals(
                456.75,
                published.y()
        );
    }

    @Test
    void handleCursor_shouldReject_whenUserCannotView() {

        when(cachingService.canView(
                boardId,
                userId
        )).thenReturn(false);

        CursorEvent event =
                new CursorEvent(
                        100,
                        200
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> controller.handleCursor(
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
    void handleCursor_shouldUseAuthenticatedUserId() {

        when(cachingService.canView(
                boardId,
                userId
        )).thenReturn(true);

        CursorEvent event =
                new CursorEvent(
                        10,
                        20
                );

        controller.handleCursor(
                boardId,
                event,
                authentication
        );

        ArgumentCaptor<CursorMovedEvent> captor =
                ArgumentCaptor.forClass(
                        CursorMovedEvent.class
                );

        verify(eventPublisher).publish(
                eq(boardId),
                eq("CURSOR_MOVED"),
                captor.capture()
        );

        assertEquals(
                userId,
                captor.getValue().userId()
        );
    }
}