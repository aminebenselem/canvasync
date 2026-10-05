package com.whiteboard.backend.auth.dto;


public record AuthResponse(String token) {

    @Override
    public String toString() {
        return "AuthResponse[token=[REDACTED]]";
    }
}