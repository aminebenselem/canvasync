package com.whiteboard.backend.auth.dto;

public record RegisterDto (
        String username,
        String email,
        String password
) {
}
