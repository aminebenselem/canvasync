package com.whiteboard.backend.element.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record DeleteElementsDto(
        @NotEmpty @NotNull List<UUID> elements
) {
}
