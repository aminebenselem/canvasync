package com.whiteboard.backend.element.dto;

import com.whiteboard.backend.element.ElementType;

public record UpdateShapeDto(
            ElementType type,
            String color,
            float  width,
            Point startPoint,
            Point endPoint,
            String text
){
}
