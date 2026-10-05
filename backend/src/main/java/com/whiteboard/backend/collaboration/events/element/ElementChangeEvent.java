package com.whiteboard.backend.collaboration.events.element;

import com.whiteboard.backend.element.dto.ElementDto;

import java.util.List;
import java.util.UUID;

public record ElementChangeEvent(
        Long actorId,
        ElementDto element,
        List<UUID> elementIds
) {}