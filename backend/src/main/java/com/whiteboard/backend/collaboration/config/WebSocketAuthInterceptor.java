package com.whiteboard.backend.collaboration;

import com.whiteboard.backend.board.BoardAccessPolicy;
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
    private final BoardAccessPolicy boardAccessPolicy;

    public WebSocketAuthInterceptor(
            JwtDecoder jwtDecoder,
            BoardAccessPolicy boardAccessPolicy
    ) {
        this.jwtDecoder = jwtDecoder;
        this.boardAccessPolicy = boardAccessPolicy;
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

            throw new IllegalArgumentException(
                    "Missing or invalid Authorization header"
            );
        }

        String token = authorization.substring(7);

        Jwt jwt = jwtDecoder.decode(token);

        // Whatever claim you use as your user ID
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
            throw new IllegalArgumentException(
                    "Unauthenticated WebSocket session"
            );
        }

        String destination = accessor.getDestination();

        if (destination == null) {
            throw new IllegalArgumentException(
                    "Missing subscription destination"
            );
        }

        String prefix = "/topic/boards/";

        if (!destination.startsWith(prefix)) {
            return;
        }

        String boardIdString =
                destination.substring(prefix.length());

        UUID boardId;

        try {
            boardId = UUID.fromString(boardIdString);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid board ID"
            );
        }

        Long userId =
                ((WebSocketAuthentication) authentication)
                        .getUserId();

        if (!boardAccessPolicy.canView(boardId, userId)) {
            throw new IllegalArgumentException(
                    "User is not allowed to subscribe to this board"
            );
        }
    }
}