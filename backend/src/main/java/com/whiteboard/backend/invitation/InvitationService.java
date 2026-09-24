package com.whiteboard.backend.auth.invitation;

import com.whiteboard.backend.auth.invitation.exception.InvalidInvitationStateException;
import com.whiteboard.backend.auth.invitation.exception.InvitationNotFoundException;
import com.whiteboard.backend.board.Board;
import com.whiteboard.backend.board.BoardAccessPolicy;
import com.whiteboard.backend.board.BoardService;
import com.whiteboard.backend.board.boardmember.BoardPermission;
import com.whiteboard.backend.board.exception.BoardAccessDeniedException;
import com.whiteboard.backend.user.User;
import com.whiteboard.backend.user.UserService;
import com.whiteboard.backend.user.exception.UserNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class InvitationService {
    private final InvitationRepository invitationRepository;
    private final UserService userService;
    private final BoardAccessPolicy boardAccessPolicy;
    private final BoardService boardService;
    public InvitationService(InvitationRepository invitationRepository, UserService userService, BoardAccessPolicy boardAccessPolicy, BoardService boardService) {
        this.invitationRepository = invitationRepository;
        this.userService = userService;
        this.boardAccessPolicy = boardAccessPolicy;
        this.boardService = boardService;
    }
    @Transactional
    public  Invitation sendInvitation(String userEmail,UUID boardId ,Long senderId, BoardPermission permission) {
        if (!boardAccessPolicy.isOwner(boardId, senderId)) {
            throw new BoardAccessDeniedException("User does not have permission to send invitations for this board.");
        }
        User user = userService.getUserByEmail(userEmail).orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));
        User sender = userService.getUserEntity(senderId);
        if (user ==sender){
            throw new InvalidInvitationStateException("Cannot send invitation to owner.");
        }
        Board board = boardAccessPolicy.getBoardIfAuthorized(boardId, senderId);
        Invitation invitation = new Invitation();
        invitation.setBoard(board);
        invitation.setUser(user);
        invitation.setSender(sender);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setPermission(permission);
        return invitationRepository.save(invitation);
    }
    @Transactional
    public Invitation acceptInvitation(UUID invitationId, Long userId ) {
        Invitation invitation = getInvitation(invitationId);
        if (!invitation.getUser().getId().equals(userId)) {
            throw new InvitationNotFoundException("User does not have permission to accept this invitation.");
        }
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new InvalidInvitationStateException(
                    "Invitation is not pending."
            );
        }
        invitation.setStatus(InvitationStatus.ACCEPTED);
         boardService.addMemberInternal(invitation.getBoard().getId(),userId, invitation.getPermission());
        return invitationRepository.save(invitation);
    }
    @Transactional
    public Invitation declineInvitation(UUID invitationId, Long userId) {
        Invitation invitation = getInvitation(invitationId);
        if (!invitation.getUser().getId().equals(userId)) {
            throw new InvitationNotFoundException("User does not have permission to decline this invitation.");
        }
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new InvalidInvitationStateException(
                    "Invitation is not pending."
            );
        }
        invitation.setStatus(InvitationStatus.DECLINED);
        return invitationRepository.save(invitation);
    }

    public List<Invitation> getMyInvitations(Long userId) {
        return invitationRepository.findByUserId(userId);
    }
    private Invitation getInvitation(UUID invitationId) {
        return invitationRepository.findById(invitationId)
                .orElseThrow(() ->
                        new InvitationNotFoundException(
                                "Invitation not found with id: " + invitationId
                        ));
    }
}
