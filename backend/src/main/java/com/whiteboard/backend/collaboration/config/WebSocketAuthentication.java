package com.whiteboard.backend.collaboration.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collections;

public class WebSocketAuthentication extends AbstractAuthenticationToken {

    private final Long userId;
    private final Jwt jwt;

    public WebSocketAuthentication(
            Long userId,
            Jwt jwt
    ) {
        super(Collections.emptyList());
        this.userId = userId;
        this.jwt = jwt;
        setAuthenticated(true);
    }

    public Long getUserId() {
        return userId;
    }

    public Jwt getJwt() {
        return jwt;
    }

    @Override
    public Object getCredentials() {
        return jwt.getTokenValue();
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }
}