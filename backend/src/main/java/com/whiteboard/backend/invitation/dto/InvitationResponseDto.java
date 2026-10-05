package com.whiteboard.backend.invitation.dto;

import com.whiteboard.backend.invitation.InvitationStatus;
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