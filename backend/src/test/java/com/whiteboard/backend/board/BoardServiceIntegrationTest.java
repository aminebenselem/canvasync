package com.whiteboard.backend.board;

import com.whiteboard.backend.board.boardmember.BoardMember;
import com.whiteboard.backend.board.boardmember.BoardMemberRepository;
import com.whiteboard.backend.board.boardmember.BoardPermission;
import com.whiteboard.backend.board.dto.AddMemberDto;
import com.whiteboard.backend.board.dto.BoardMemberDto;
import com.whiteboard.backend.board.dto.UpdateMemberPermissionDto;
import com.whiteboard.backend.board.exception.BoardAccessDeniedException;
import com.whiteboard.backend.board.exception.InvalidMembershipException;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class BoardServiceIntegrationTest {

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
    BoardService boardService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    BoardRepository boardRepository;

    @Autowired
    BoardMemberRepository boardMemberRepository;

    @Test
    void createBoard_shouldPersistBoardWithOwner() {

        User owner = new User();
        owner.setUsername("service-owner");
        owner.setEmail("service-owner@test.com");
        owner.setPassword("password");

        owner = userRepository.save(owner);

        Board board =
                boardService.createBoard(
                        "My Integration Board",
                        owner.getId()
                );

        assertNotNull(board.getId());
        assertEquals("My Integration Board", board.getName());
        assertEquals(owner.getId(), board.getOwner().getId());

        assertTrue(boardRepository.existsById(board.getId()));
    }
    @Test
    void getBoard_shouldReturnBoardForOwner() {

        User owner = new User();
        owner.setUsername("get-board-owner");
        owner.setEmail("get-board-owner@test.com");
        owner.setPassword("password");

        owner = userRepository.save(owner);

        Board board = boardService.createBoard(
                "My Board",
                owner.getId()
        );

        Board result = boardService.getBoard(
                board.getId(),
                owner.getId()
        );

        assertNotNull(result);
        assertEquals(board.getId(), result.getId());
        assertEquals("My Board", result.getName());
        assertEquals(owner.getId(), result.getOwner().getId());
    }
    @Test
    void getBoard_shouldThrowForUnauthorizedUser() {

        User owner = new User();
        owner.setUsername("authorized-owner");
        owner.setEmail("authorized-owner@test.com");
        owner.setPassword("password");
        owner = userRepository.save(owner);

        User randomUser = new User();
        randomUser.setUsername("unauthorized-user");
        randomUser.setEmail("unauthorized-user@test.com");
        randomUser.setPassword("password");
        randomUser = userRepository.save(randomUser);

        Board board = boardService.createBoard(
                "Private Board",
                owner.getId()
        );

        User finalRandomUser = randomUser;
        assertThrows(
                BoardAccessDeniedException.class,
                () -> boardService.getBoard(
                        board.getId(),
                        finalRandomUser.getId()
                )
        );
    }
    @Test
    void getUserBoards_shouldReturnOwnedBoards() {

        User owner = new User();
        owner.setUsername("boards-owner");
        owner.setEmail("boards-owner@test.com");
        owner.setPassword("password");
        owner = userRepository.save(owner);

        Board board = boardService.createBoard(
                "Owned Board",
                owner.getId()
        );

        List<Board> result =
                boardService.getUserBoards(owner.getId());

        assertEquals(1, result.size());
        assertEquals(board.getId(), result.get(0).getId());
    }
    @Test
    void getUserBoards_shouldReturnOwnedAndMemberBoards() {

        User owner = new User();
        owner.setUsername("board-owner");
        owner.setEmail("board-owner@test.com");
        owner.setPassword("password");
        owner = userRepository.save(owner);

        User member = new User();
        member.setUsername("board-member");
        member.setEmail("board-member@test.com");
        member.setPassword("password");
        member = userRepository.save(member);

        Board ownedBoard = boardService.createBoard(
                "Owned Board",
                member.getId()
        );

        Board memberBoard = boardService.createBoard(
                "Member Board",
                owner.getId()
        );

        BoardMember membership = new BoardMember();
        membership.setBoard(memberBoard);
        membership.setUser(member);
        membership.setPermission(BoardPermission.VIEWER);

        boardMemberRepository.save(membership);

        List<Board> result =
                boardService.getUserBoards(member.getId());

        assertEquals(2, result.size());

        assertTrue(
                result.stream()
                        .anyMatch(board -> board.getId().equals(ownedBoard.getId()))
        );

        assertTrue(
                result.stream()
                        .anyMatch(board -> board.getId().equals(memberBoard.getId()))
        );
    }

    @Test
    void addMember_shouldCreateMembershipForOwner() {

        User owner = createUser("add-owner", "add-owner@test.com");
        User member = createUser("add-member", "add-member@test.com");

        Board board = boardService.createBoard("Board", owner.getId());

        AddMemberDto dto =
                new AddMemberDto(member.getId(), BoardPermission.EDITOR);

        boardService.addMember(board.getId(), owner.getId(), dto);

        Optional<BoardMember> result =
                boardMemberRepository.findByBoardIdAndUserId(
                        board.getId(),
                        member.getId()
                );

        assertTrue(result.isPresent());
        assertEquals(BoardPermission.EDITOR, result.get().getPermission());
    }

    @Test
    void addMember_shouldRejectNonOwner() {

        User owner = createUser("add2-owner", "add2-owner@test.com");
        User member = createUser("add2-member", "add2-member@test.com");
        User targetmember = createUser("add3-member", "add3-member@test.com");

        Board board = boardService.createBoard("Board", owner.getId());

        AddMemberDto dto =
                new AddMemberDto(member.getId(), BoardPermission.VIEWER);

        assertThrows(
                BoardAccessDeniedException.class,
                () -> boardService.addMember(
                        board.getId(),
                        targetmember.getId(),
                        dto
                )
        );

        assertTrue(
                boardMemberRepository
                        .findByBoardIdAndUserId(board.getId(), member.getId())
                        .isEmpty()
        );
    }

    @Test
    void addMember_shouldRejectOwnerAddingThemselves() {

        User owner = createUser("self-owner", "self-owner@test.com");

        Board board = boardService.createBoard("Board", owner.getId());

        AddMemberDto dto =
                new AddMemberDto(owner.getId(), BoardPermission.EDITOR);

        assertThrows(
                InvalidMembershipException.class,
                () -> boardService.addMember(
                        board.getId(),
                        owner.getId(),
                        dto
                )
        );
    }

    @Test
    void removeMember_shouldRemoveMembershipForOwner() {

        User owner = createUser("remove-owner", "remove-owner@test.com");
        User member = createUser("remove-member", "remove-member@test.com");

        Board board = boardService.createBoard("Board", owner.getId());

        BoardMember membership = new BoardMember();
        membership.setBoard(board);
        membership.setUser(member);
        membership.setPermission(BoardPermission.EDITOR);
        boardMemberRepository.save(membership);

        boardService.removeMember(
                board.getId(),
                owner.getId(),
                member.getId()
        );

        assertTrue(
                boardMemberRepository
                        .findByBoardIdAndUserId(board.getId(), member.getId())
                        .isEmpty()
        );
    }
    @Test
    void updateMemberPermission_shouldUpdatePermissionForOwner() {

        User owner = createUser("permission-owner", "permission-owner@test.com");
        User member = createUser("permission-member", "permission-member@test.com");

        Board board = boardService.createBoard("Board", owner.getId());

        BoardMember membership = new BoardMember();
        membership.setBoard(board);
        membership.setUser(member);
        membership.setPermission(BoardPermission.VIEWER);
        boardMemberRepository.save(membership);

        UpdateMemberPermissionDto dto =
                new UpdateMemberPermissionDto(
                        member.getId(),
                        BoardPermission.EDITOR
                );

        boardService.updateMemberPermission(
                board.getId(),
                owner.getId(),
                dto
        );

        BoardMember result =
                boardMemberRepository
                        .findByBoardIdAndUserId(board.getId(), member.getId())
                        .orElseThrow();

        assertEquals(BoardPermission.EDITOR, result.getPermission());
    }
    @Test
    void listBoardMembers_shouldReturnMembersForAuthorizedUser() {

        User owner = createUser("list-owner", "list-owner@test.com");
        User member = createUser("list-member", "list-member@test.com");

        Board board = boardService.createBoard("Board", owner.getId());

        BoardMember membership = new BoardMember();
        membership.setBoard(board);
        membership.setUser(member);
        membership.setPermission(BoardPermission.VIEWER);
        boardMemberRepository.save(membership);

        List<BoardMemberDto> result =
                boardService.listBoardMembers(
                        board.getId(),
                        owner.getId()
                );

        assertEquals(1, result.size());
        assertEquals(member.getId(), result.get(0).userId());
        assertEquals(BoardPermission.VIEWER, result.get(0).permission());
    }
    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password");
        return userRepository.save(user);
    }
    @BeforeEach
    void cleanDatabase() {
        boardRepository.deleteAll();
        userRepository.deleteAll();
    }
}