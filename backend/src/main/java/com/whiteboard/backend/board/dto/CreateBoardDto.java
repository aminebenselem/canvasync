package com.whiteboard.backend.board.dto;

public record CreateBoardDto(
        @NotBlank
        @Size(max = 255)
        String name
) {}