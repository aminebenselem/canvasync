package com.whiteboard.backend.board.dto;

import com.whiteboard.backend.board.boardmember.BoardPermission;

public record AddMemberDto(
        Long memberId,
        BoardPermission permission
) {}