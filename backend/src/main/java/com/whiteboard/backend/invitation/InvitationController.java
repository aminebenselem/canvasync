package com.whiteboard.backend.auth.invitation;

import com.whiteboard.backend.auth.invitation.dto.InvitationRequestDto;
import com.whiteboard.backend.auth.invitation.dto.InvitationResponseDto;
import com.whiteboard.backend.auth.invitation.mapper.InvitationMapper;
import com.whiteboard.backend.board.boardmember.BoardPermission;
import com.whiteboard.backend.board.exception.UnauthorizedUserException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @GetMapping
    public List<InvitationResponseDto> getMyInvitations(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = getAuthenticatedUserId(jwt);

        return InvitationMapper.toDtoList(
                invitationService.getMyInvitations(userId)
        );
    }

    @PostMapping("/board/{boardId}")
    public InvitationResponseDto sendInvitation(
            @PathVariable UUID boardId,
            @RequestBody InvitationRequestDto request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long senderId = getAuthenticatedUserId(jwt);

        return InvitationMapper.toDto(
                invitationService.sendInvitation(
                        request.email(),
                        boardId,
                        senderId,
                        request.permission()
                )
        );
    }

    @PostMapping("/{invitationId}/accept")
    public InvitationResponseDto acceptInvitation(
            @PathVariable UUID invitationId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = getAuthenticatedUserId(jwt);

        return InvitationMapper.toDto(
                invitationService.acceptInvitation(
                        invitationId,
                        userId
                )
        );
    }

    @PostMapping("/{invitationId}/decline")
    public InvitationResponseDto declineInvitation(
            @PathVariable UUID invitationId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = getAuthenticatedUserId(jwt);

        return InvitationMapper.toDto(
                invitationService.declineInvitation(
                        invitationId,
                        userId
                )
        );
    }

    private Long getAuthenticatedUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new UnauthorizedUserException(
                    "User is not authenticated"
            );
        }

        return Long.valueOf(jwt.getSubject());
    }
}