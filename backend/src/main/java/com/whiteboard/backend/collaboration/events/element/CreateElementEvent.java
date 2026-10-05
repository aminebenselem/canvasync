package com.whiteboard.backend.collaboration.events.element;

import com.whiteboard.backend.element.dto.CreateShapeDto;
import com.whiteboard.backend.element.dto.CreateStrokeDto;

public record CreateElementEvent(
        String elementType,
        CreateShapeDto shape,
        CreateStrokeDto stroke
) {}