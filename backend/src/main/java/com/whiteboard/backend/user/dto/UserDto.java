package com.whiteboard.backend.user.dto;

public record UserDto(
        Long id,
        String username,
        String email
) {
}
