package com.whiteboard.backend.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBoardDto(
        @NotBlank(message = "Board name is required")
        @Size(max = 255, message = "Board name must not exceed 255 characters")
        String name
) {}