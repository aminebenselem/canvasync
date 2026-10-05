package com.whiteboard.backend.invitation;

import com.whiteboard.backend.board.Board;
import com.whiteboard.backend.board.access.BoardAccessPolicy;
import com.whiteboard.backend.board.BoardService;
import com.whiteboard.backend.board.boardmember.BoardMember;
import com.whiteboard.backend.board.boardmember.BoardMemberRepository;
import com.whiteboard.backend.board.boardmember.BoardPermission;
import com.whiteboard.backend.board.exception.BoardAccessDeniedException;
import com.whiteboard.backend.invitation.exception.InvalidInvitationStateException;
import com.whiteboard.backend.invitation.exception.InvitationNotFoundException;
import com.whiteboard.backend.user.User;
import com.whiteboard.backend.user.UserService;
import com.whiteboard.backend.user.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private UserService userService;

    @Mock
    private BoardAccessPolicy boardAccessPolicy;

    @Mock
    private BoardService boardService;

    @Mock
    private BoardMemberRepository boardMemberRepository;

    @InjectMocks
    private InvitationService invitationService;


    @Test
    void sendInvitation_shouldThrowWhenUserIsNotOwner() {
        UUID boardId = UUID.randomUUID();
        Long senderId = 1L;

        when(boardAccessPolicy.isOwner(boardId, senderId))
                .thenReturn(false);

        assertThrows(
                BoardAccessDeniedException.class,
                () -> invitationService.sendInvitation(
                        "member@example.com",
                        boardId,
                        senderId,
                        BoardPermission.EDITOR
                )
        );

        verify(invitationRepository, never())
                .save(any(Invitation.class));
    }
    @Test
    void sendInvitation_shouldThrowWhenUserDoesNotExist() {
        UUID boardId = UUID.randomUUID();
        Long senderId = 1L;

        when(boardAccessPolicy.isOwner(boardId, senderId))
                .thenReturn(true);

        when(userService.getUserByEmail("member@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> invitationService.sendInvitation(
                        "member@example.com",
                        boardId,
                        senderId,
                        BoardPermission.EDITOR
                )
        );

        verify(invitationRepository, never())
                .save(any(Invitation.class));
    }
    @Test
    void sendInvitation_shouldThrowWhenInvitingOwner() {
        UUID boardId = UUID.randomUUID();
        Long senderId = 1L;

        User sender = new User();
        sender.setId(senderId);

        when(boardAccessPolicy.isOwner(boardId, senderId))
                .thenReturn(true);

        when(userService.getUserByEmail("owner@example.com"))
                .thenReturn(Optional.of(sender));

        when(userService.getUserEntity(senderId))
                .thenReturn(sender);

        assertThrows(
                InvalidInvitationStateException.class,
                () -> invitationService.sendInvitation(
                        "owner@example.com",
                        boardId,
                        senderId,
                        BoardPermission.EDITOR
                )
        );

        verify(invitationRepository, never())
                .save(any(Invitation.class));
    }

    @Test
    void sendInvitation_shouldCreateInvitation() {
        UUID boardId = UUID.randomUUID();
        Long senderId = 1L;
        Long memberId = 2L;

        User sender = new User();
        sender.setId(senderId);

        User member = new User();
        member.setId(memberId);

        Board board = new Board();
        board.setId(boardId);

        Invitation savedInvitation = new Invitation();
        savedInvitation.setBoard(board);
        savedInvitation.setUser(member);
        savedInvitation.setSender(sender);
        savedInvitation.setPermission(BoardPermission.EDITOR);
        savedInvitation.setStatus(InvitationStatus.PENDING);

        when(boardAccessPolicy.isOwner(boardId, senderId))
                .thenReturn(true);

        when(userService.getUserByEmail("member@example.com"))
                .thenReturn(Optional.of(member));

        when(userService.getUserEntity(senderId))
                .thenReturn(sender);

        when(boardAccessPolicy.getBoardIfAuthorized(boardId, senderId))
                .thenReturn(board);

        when(invitationRepository.save(any(Invitation.class)))
                .thenReturn(savedInvitation);

        Invitation result = invitationService.sendInvitation(
                "member@example.com",
                boardId,
                senderId,
                BoardPermission.EDITOR
        );

        assertEquals(InvitationStatus.PENDING, result.getStatus());
        assertEquals(BoardPermission.EDITOR, result.getPermission());
        assertEquals(board, result.getBoard());
        assertEquals(member, result.getUser());
        assertEquals(sender, result.getSender());

        verify(invitationRepository)
                .save(any(Invitation.class));
    }
    @Test
    void acceptInvitation_shouldThrowWhenInvitationIsNotPending() {
        UUID invitationId = UUID.randomUUID();
        Long userId = 2L;

        Invitation invitation = new Invitation();
        invitation.setStatus(InvitationStatus.ACCEPTED);
        User user = new User();
        user.setId(userId);

        invitation.setUser(user);
        invitation.setStatus(InvitationStatus.ACCEPTED);
        when(invitationRepository.findById(invitationId))
                .thenReturn(Optional.of(invitation));

        assertThrows(
                IllegalStateException.class,
                () -> invitationService.acceptInvitation(
                        invitationId,
                        userId
                )
        );

        verify(boardMemberRepository, never())
                .save(any(BoardMember.class));
    }
    @Test
    void acceptInvitation_shouldUpdateExistingMemberPermission() {

        UUID invitationId = UUID.randomUUID();
        Long userId = 2L;
        UUID boardId = UUID.randomUUID();

        Board board = new Board();
        board.setId(boardId);

        User user = new User();
        user.setId(userId);

        Invitation invitation = new Invitation();
        invitation.setBoard(board);
        invitation.setUser(user);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setPermission(BoardPermission.EDITOR);

        BoardMember member = new BoardMember();
        member.setPermission(BoardPermission.VIEWER);

        when(invitationRepository.findById(invitationId))
                .thenReturn(Optional.of(invitation));

        when(boardMemberRepository.findByBoardIdAndUserId(
                boardId,
                userId
        )).thenReturn(Optional.of(member));

        Invitation result =
                invitationService.acceptInvitation(
                        invitationId,
                        userId
                );

        assertEquals(BoardPermission.EDITOR, member.getPermission());
        assertEquals(InvitationStatus.ACCEPTED, result.getStatus());

        verify(boardMemberRepository)
                .save(member);
    }
    @Test
    void acceptInvitation_shouldCreateMemberWhenUserIsNotAlreadyMember() {
        UUID invitationId = UUID.randomUUID();
        Long userId = 2L;
        UUID boardId = UUID.randomUUID();

        Board board = new Board();
        board.setId(boardId);

        User user = new User();
        user.setId(userId);

        Invitation invitation = new Invitation();
        invitation.setBoard(board);
        invitation.setUser(user);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setPermission(BoardPermission.EDITOR);

        when(invitationRepository.findById(invitationId))
                .thenReturn(Optional.of(invitation));

        when(boardMemberRepository.findByBoardIdAndUserId(
                boardId,
                userId
        )).thenReturn(Optional.empty());

        Invitation result =
                invitationService.acceptInvitation(
                        invitationId,
                        userId
                );

        assertEquals(
                InvitationStatus.ACCEPTED,
                result.getStatus()
        );

        verify(boardMemberRepository)
                .save(any(BoardMember.class));
    }

    @Test
    void declineInvitation_shouldThrowWhenUserDoesNotOwnInvitation() {
        UUID invitationId = UUID.randomUUID();
        Long invitedUserId = 2L;
        Long otherUserId = 3L;

        User invitedUser = new User();
        invitedUser.setId(invitedUserId);

        Invitation invitation = new Invitation();
        invitation.setUser(invitedUser);
        invitation.setStatus(InvitationStatus.PENDING);

        when(invitationRepository.findById(invitationId))
                .thenReturn(Optional.of(invitation));

        assertThrows(
                InvitationNotFoundException.class,
                () -> invitationService.declineInvitation(
                        invitationId,
                        otherUserId
                )
        );

        verify(invitationRepository, never())
                .save(any(Invitation.class));
    }

    @Test
    void declineInvitation_shouldThrowWhenInvitationIsNotPending() {
        UUID invitationId = UUID.randomUUID();
        Long userId = 2L;

        User user = new User();
        user.setId(userId);

        Invitation invitation = new Invitation();
        invitation.setUser(user);
        invitation.setStatus(InvitationStatus.ACCEPTED);

        when(invitationRepository.findById(invitationId))
                .thenReturn(Optional.of(invitation));

        assertThrows(
                InvalidInvitationStateException.class,
                () -> invitationService.declineInvitation(
                        invitationId,
                        userId
                )
        );
    }
    @Test
    void declineInvitation_shouldDeclinePendingInvitation() {
        UUID invitationId = UUID.randomUUID();
        Long userId = 2L;

        User user = new User();
        user.setId(userId);

        Invitation invitation = new Invitation();
        invitation.setUser(user);
        invitation.setStatus(InvitationStatus.PENDING);

        when(invitationRepository.findById(invitationId))
                .thenReturn(Optional.of(invitation));

        when(invitationRepository.save(invitation))
                .thenReturn(invitation);

        Invitation result =
                invitationService.declineInvitation(
                        invitationId,
                        userId
                );

        assertEquals(
                InvitationStatus.DECLINED,
                result.getStatus()
        );

        verify(invitationRepository)
                .save(invitation);
    }
    @Test
    void getMyInvitations_shouldReturnUserInvitations() {
        Long userId = 2L;

        List<Invitation> invitations = List.of(
                new Invitation(),
                new Invitation()
        );

        when(invitationRepository.findByUserId(userId))
                .thenReturn(invitations);

        List<Invitation> result =
                invitationService.getMyInvitations(userId);

        assertEquals(invitations, result);

        verify(invitationRepository)
                .findByUserId(userId);
    }
}