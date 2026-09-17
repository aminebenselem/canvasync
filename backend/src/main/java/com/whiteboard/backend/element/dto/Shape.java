package com.whiteboard.backend.element.dto;

public record Shape (
        Point startPoint,
        Point endPoint,
        String color,
        String type,
        String text
){
}
