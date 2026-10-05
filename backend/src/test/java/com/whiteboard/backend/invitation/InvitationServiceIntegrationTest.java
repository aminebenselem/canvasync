package com.whiteboard.backend.invitation;
import com.whiteboard.backend.board.Board;
import com.whiteboard.backend.board.BoardRepository;
import com.whiteboard.backend.board.boardmember.BoardMember;
import com.whiteboard.backend.board.boardmember.BoardMemberRepository;
import com.whiteboard.backend.board.boardmember.BoardPermission;
import com.whiteboard.backend.board.exception.BoardAccessDeniedException;
import com.whiteboard.backend.invitation.exception.InvalidInvitationStateException;
import com.whiteboard.backend.invitation.exception.InvitationNotFoundException;
import com.whiteboard.backend.user.User;
import com.whiteboard.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class InvitationServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    InvitationService invitationService;

    @Autowired
    InvitationRepository invitationRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    BoardRepository boardRepository;

    @Autowired
    BoardMemberRepository boardMemberRepository;

    @BeforeEach
    void cleanDatabase() {
        invitationRepository.deleteAll();
        boardMemberRepository.deleteAll();
        boardRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password");
        return userRepository.save(user);
    }

    @Test
    void sendInvitation_shouldPersistPendingInvitation() {

        User owner = createUser("invite-owner", "invite-owner@test.com");
        User recipient = createUser("invite-recipient", "invite-recipient@test.com");

        Board board = new Board();
        board.setName("Invitation Board");
        board.setOwner(owner);
        board = boardRepository.save(board);

        Invitation invitation =
                invitationService.sendInvitation(
                        recipient.getEmail(),
                        board.getId(),
                        owner.getId(),
                        BoardPermission.EDITOR
                );

        assertNotNull(invitation.getId());
        assertEquals(InvitationStatus.PENDING, invitation.getStatus());
        assertEquals(recipient.getId(), invitation.getUser().getId());
        assertEquals(owner.getId(), invitation.getSender().getId());
        assertEquals(BoardPermission.EDITOR, invitation.getPermission());

        assertTrue(invitationRepository.existsById(invitation.getId()));
    }

    @Test
    void sendInvitation_shouldRejectNonOwner() {

        User owner = createUser("non-owner-board-owner", "non-owner-board-owner@test.com");
        User sender = createUser("non-owner-sender", "non-owner-sender@test.com");
        User recipient = createUser("non-owner-recipient", "non-owner-recipient@test.com");

        Board board = new Board();
        board.setName("Board");
        board.setOwner(owner);
        board = boardRepository.save(board);

        Board finalBoard = board;
        assertThrows(
                BoardAccessDeniedException.class,
                () -> invitationService.sendInvitation(
                        recipient.getEmail(),
                        finalBoard.getId(),
                        sender.getId(),
                        BoardPermission.VIEWER
                )
        );

        assertTrue(invitationRepository.findAll().isEmpty());
    }

    @Test
    void sendInvitation_shouldRejectInvitationToOwner() {

        User owner = createUser("owner-target", "owner-target@test.com");

        Board board = new Board();
        board.setName("Board");
        board.setOwner(owner);
        board = boardRepository.save(board);

        Board finalBoard = board;
        assertThrows(
                InvalidInvitationStateException.class,
                () -> invitationService.sendInvitation(
                        owner.getEmail(),
                        finalBoard.getId(),
                        owner.getId(),
                        BoardPermission.VIEWER
                )
        );

        assertTrue(invitationRepository.findAll().isEmpty());
    }

    @Test
    void acceptInvitation_shouldCreateMembership() {

        User owner = createUser("accept-owner", "accept-owner@test.com");
        User recipient = createUser("accept-recipient", "accept-recipient@test.com");

        Board board = new Board();
        board.setName("Board");
        board.setOwner(owner);
        board = boardRepository.save(board);

        Invitation invitation = new Invitation();
        invitation.setBoard(board);
        invitation.setUser(recipient);
        invitation.setSender(owner);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setPermission(BoardPermission.EDITOR);
        invitation = invitationRepository.save(invitation);

        Invitation result =
                invitationService.acceptInvitation(
                        invitation.getId(),
                        recipient.getId()
                );

        assertEquals(InvitationStatus.ACCEPTED, result.getStatus());

        BoardMember member =
                boardMemberRepository
                        .findByBoardIdAndUserId(
                                board.getId(),
                                recipient.getId()
                        )
                        .orElseThrow();

        assertEquals(BoardPermission.EDITOR, member.getPermission());
    }

    @Test
    void acceptInvitation_shouldUpdateExistingMembershipPermission() {

        User owner = createUser("accept-existing-owner", "accept-existing-owner@test.com");
        User member = createUser("accept-existing-member", "accept-existing-member@test.com");

        Board board = new Board();
        board.setName("Board");
        board.setOwner(owner);
        board = boardRepository.save(board);

        BoardMember existingMember = new BoardMember();
        existingMember.setBoard(board);
        existingMember.setUser(member);
        existingMember.setPermission(BoardPermission.VIEWER);
        boardMemberRepository.save(existingMember);

        Invitation invitation = new Invitation();
        invitation.setBoard(board);
        invitation.setUser(member);
        invitation.setSender(owner);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setPermission(BoardPermission.EDITOR);
        invitation = invitationRepository.save(invitation);

        invitationService.acceptInvitation(
                invitation.getId(),
                member.getId()
        );

        BoardMember result =
                boardMemberRepository
                        .findByBoardIdAndUserId(
                                board.getId(),
                                member.getId()
                        )
                        .orElseThrow();

        assertEquals(BoardPermission.EDITOR, result.getPermission());
    }

    @Test
    void declineInvitation_shouldPersistDeclinedStatus() {

        User owner = createUser("decline-owner", "decline-owner@test.com");
        User recipient = createUser("decline-recipient", "decline-recipient@test.com");

        Board board = new Board();
        board.setName("Board");
        board.setOwner(owner);
        board = boardRepository.save(board);

        Invitation invitation = new Invitation();
        invitation.setBoard(board);
        invitation.setUser(recipient);
        invitation.setSender(owner);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setPermission(BoardPermission.VIEWER);
        invitation = invitationRepository.save(invitation);

        Invitation result =
                invitationService.declineInvitation(
                        invitation.getId(),
                        recipient.getId()
                );

        assertEquals(InvitationStatus.DECLINED, result.getStatus());

        Invitation saved =
                invitationRepository.findById(invitation.getId())
                        .orElseThrow();

        assertEquals(InvitationStatus.DECLINED, saved.getStatus());
    }

    @Test
    void getMyInvitations_shouldReturnUserInvitations() {

        User owner = createUser("my-invites-owner", "my-invites-owner@test.com");
        User recipient = createUser("my-invites-recipient", "my-invites-recipient@test.com");

        Board board = new Board();
        board.setName("Board");
        board.setOwner(owner);
        board = boardRepository.save(board);

        Invitation invitation = new Invitation();
        invitation.setBoard(board);
        invitation.setUser(recipient);
        invitation.setSender(owner);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setPermission(BoardPermission.VIEWER);
        invitationRepository.save(invitation);

        List<Invitation> result =
                invitationService.getMyInvitations(recipient.getId());

        assertEquals(1, result.size());
        assertEquals(
                InvitationStatus.PENDING,
                result.get(0).getStatus()
        );
    }
    @Test
    void acceptInvitation_shouldRejectDifferentUser() {

        User owner = createUser(
                "my-invites-owner",
                "my-invites-owner@test.com"
        );

        User recipient = createUser(
                "my-invites-recipient",
                "my-invites-recipient@test.com"
        );

        User randomUser = createUser(
                "my-invites-randomuser",
                "my-invites-randomuser@test.com"
        );

        Board board = new Board();
        board.setName("Board");
        board.setOwner(owner);
        board = boardRepository.save(board);

        Invitation invitation = new Invitation();
        invitation.setBoard(board);
        invitation.setUser(recipient);
        invitation.setSender(owner);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setPermission(BoardPermission.VIEWER);

        invitation = invitationRepository.save(invitation);

        Invitation finalInvitation = invitation;
        assertThrows(
                InvitationNotFoundException.class,
                () -> invitationService.acceptInvitation(
                        finalInvitation.getId(),
                        randomUser.getId()
                )
        );

        Invitation saved =
                invitationRepository.findById(invitation.getId())
                        .orElseThrow();

        assertEquals(InvitationStatus.PENDING, saved.getStatus());
    }
}