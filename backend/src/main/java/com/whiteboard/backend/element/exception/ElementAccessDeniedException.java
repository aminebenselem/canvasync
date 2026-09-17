package com.whiteboard.backend.element.exception;

public class ElementAccessDeniedException extends RuntimeException {
    public ElementAccessDeniedException(String message) {
        super(message);
    }
}
