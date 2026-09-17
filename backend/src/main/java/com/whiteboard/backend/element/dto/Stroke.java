package com.whiteboard.backend.element.dto;

public record Stroke(
        String color,
        float width,
        double[] points
) {
}
