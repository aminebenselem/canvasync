package com.whiteboard.backend.element.dto;

import java.util.Map;
import java.util.UUID;

public record ElementDto (
        UUID id,
        String type,
        Map<String, Object> data
) {

}
