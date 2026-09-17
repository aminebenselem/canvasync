package com.whiteboard.backend.element.dto;

public record UpdateStrokeDto(
        String color,
        float width,
        Point[] points
) {

}
