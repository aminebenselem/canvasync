package com.whiteboard.backend.auth.invitation.exception;

public class InvalidInvitationStateException extends RuntimeException {
    public InvalidInvitationStateException(String message) {
        super(message);
    }
}
