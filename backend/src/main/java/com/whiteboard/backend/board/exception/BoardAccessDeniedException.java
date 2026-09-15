package com.whiteboard.backend.board.exception;

public class BoardAccessDeniedException extends RuntimeException {
    public BoardAccessDeniedException(String message) {
        super(message);
    }
}
