package com.whiteboard.backend.board.dto;

import com.whiteboard.backend.board.boardmember.BoardPermission;

public record BoardMemberDto(
        Long userId,
        String username,
        String email,
        BoardPermission permission
) {}