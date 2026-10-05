package com.whiteboard.backend.collaboration.events.drawing;

import java.util.List;

public record DrawingEvent(
        String action,
        String elementType,
        String elementId,
        String operation,
        String shapeType,
        PointDto point,
        PointDto startPoint,
        PointDto endPoint,
        String color,
        Double width,
        List<PointDto> points
) {

    public record PointDto(
            double x,
            double y
    ) {}
}