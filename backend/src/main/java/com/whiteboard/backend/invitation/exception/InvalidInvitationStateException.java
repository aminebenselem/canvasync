package com.whiteboard.backend.invitation.exception;

public class InvalidInvitationStateException extends RuntimeException {
    public InvalidInvitationStateException(String message) {
        super(message);
    }
}
