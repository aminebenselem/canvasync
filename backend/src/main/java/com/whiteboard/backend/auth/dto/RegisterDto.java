package com.whiteboard.backend.auth.dto;

public record RegisterDto (

        String username,
        String email,
        String password
) {



    @Override
    public String toString() {
        return "RegisterDto{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", password='[REDACTED]'" +
                '}';
    }
}
