package com.whiteboard.backend.element.dto;

import com.whiteboard.backend.element.ElementType;

public record CreateShapeDto(
        ElementType type,
        String text,
        Point startPoint,
        Point endPoint,
        String color,
        float width) {

}
