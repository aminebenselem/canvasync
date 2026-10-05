package com.whiteboard.backend.collaboration.config;

import com.whiteboard.backend.board.access.BoardAccessPolicyCachingService;
import com.whiteboard.backend.board.exception.BoardAccessDeniedException;
import com.whiteboard.backend.board.exception.UnauthorizedUserException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final BoardAccessPolicyCachingService cachingService;

    public WebSocketAuthInterceptor(
            JwtDecoder jwtDecoder,
            BoardAccessPolicyCachingService cachingService
    ) {
        this.jwtDecoder = jwtDecoder;
        this.cachingService = cachingService;
    }

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel
    ) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }

        return message;
    }

    private void authenticate(
            StompHeaderAccessor accessor
    ) {

        String authorization =
                accessor.getFirstNativeHeader("Authorization");

        if (authorization == null ||
                !authorization.startsWith("Bearer ")) {

            throw new UnauthorizedUserException(
                    "Missing or invalid Authorization header"
            );
        }

        String token = authorization.substring(7);

        Jwt jwt = jwtDecoder.decode(token);

        Long userId = Long.valueOf(
                jwt.getSubject()
        );

        WebSocketAuthentication authentication =
                new WebSocketAuthentication(
                        userId,
                        jwt
                );

        accessor.setUser(authentication);
    }

    private void authorizeSubscription(
            StompHeaderAccessor accessor
    ) {

        Authentication authentication =
                (Authentication) accessor.getUser();

        if (authentication == null) {
            throw new UnauthorizedUserException(
                    "Unauthenticated WebSocket session"
            );
        }

        String destination =
                accessor.getDestination();

        if (destination == null) {
            throw new IllegalArgumentException(
                    "Missing subscription destination"
            );
        }

        String prefix =
                "/topic/boards/";

        if (!destination.startsWith(prefix)) {
            return;
        }

        String[] parts =
                destination
                        .substring(prefix.length())
                        .split("/");

        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "Invalid board subscription destination"
            );
        }

        String channel = parts[1];

        if (!channel.equals("cursor")
                && !channel.equals("cursor-left")
                && !channel.equals("elements")
                && !channel.equals("drawing")) {

            throw new IllegalArgumentException(
                    "Invalid board subscription channel"
            );
        }

        UUID boardId;

        try {

            boardId =
                    UUID.fromString(parts[0]);

        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException(
                    "Invalid board ID"
            );
        }

        Long userId =
                ((WebSocketAuthentication) authentication)
                        .getUserId();

        cachingService.cachePermission(
                boardId,
                userId
        );

        if (!cachingService.canView(
                boardId,
                userId
        )) {

            throw new BoardAccessDeniedException(
                    "User cannot view this board"
            );
        }

        accessor.getSessionAttributes()
                .put(
                        "boardId",
                        boardId.toString()
                );
    }
}