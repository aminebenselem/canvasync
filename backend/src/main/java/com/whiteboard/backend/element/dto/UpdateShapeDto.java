package com.whiteboard.backend.element.dto;

public record UpdateShapeDto(
            String color,
            float  width,
            Point startPoint,
            Point endPoint,
            String text
){
}
