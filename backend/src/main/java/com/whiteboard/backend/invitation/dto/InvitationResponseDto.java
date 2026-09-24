package com.whiteboard.backend.auth.invitation.dto;

import com.whiteboard.backend.auth.invitation.InvitationStatus;
import com.whiteboard.backend.board.boardmember.BoardPermission;

import java.util.UUID;

public record InvitationResponseDto(
        UUID id,
        UUID boardId,
        Long senderId,
        Long userId,
        BoardPermission permission,
        InvitationStatus status
) {}