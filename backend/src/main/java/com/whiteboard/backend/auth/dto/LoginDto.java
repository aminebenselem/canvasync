package com.whiteboard.backend.auth.dto;
public record LoginDto(
        String email,
        String password
) {
    @Override
    public String toString() {
        return "LoginDto[email=" + email + ", password=[REDACTED]]";
    }
}
