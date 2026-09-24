package com.whiteboard.backend.auth.invitation.dto;

import com.whiteboard.backend.board.boardmember.BoardPermission;

import java.util.UUID;

public record InvitationRequestDto(
    String email,
    BoardPermission permission
) {
}
