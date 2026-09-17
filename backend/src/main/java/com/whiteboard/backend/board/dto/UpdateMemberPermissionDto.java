package com.whiteboard.backend.board.dto;

import com.whiteboard.backend.board.boardmember.BoardPermission;

public record UpdateMemberPermissionDto(
        Long memberId,
        BoardPermission permission) {
}
