package com.whiteboard.backend.element.dto;

public record CreateStrokeDto(
        String color,
        float width,
        Point[] points

) {
}
