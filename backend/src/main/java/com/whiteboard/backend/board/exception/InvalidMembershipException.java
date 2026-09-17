package com.whiteboard.backend.board.exception;

public class InvalidMembershipException extends RuntimeException {
    public InvalidMembershipException(String message) {
        super(message);
    }
}
