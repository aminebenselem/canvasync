package com.whiteboard.backend.invitation.mapper;

import com.whiteboard.backend.invitation.Invitation;
import com.whiteboard.backend.invitation.dto.InvitationResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InvitationMapper {

    public static InvitationResponseDto toDto(Invitation invitation) {
        return new InvitationResponseDto(
                invitation.getId(),
                invitation.getBoard().getId(),
                invitation.getSender().getId(),
                invitation.getUser().getId(),
                invitation.getPermission(),
                invitation.getStatus()
        );
    }

    public static List<InvitationResponseDto> toDtoList(
            List<Invitation> invitations
    ) {
        return invitations.stream()
                .map(InvitationMapper::toDto)
                .toList();
    }
}
