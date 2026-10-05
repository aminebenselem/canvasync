package com.whiteboard.backend.board;

import com.whiteboard.backend.board.boardmember.BoardMember;
import com.whiteboard.backend.board.boardmember.BoardMemberRepository;
import com.whiteboard.backend.board.boardmember.BoardPermission;
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
class BoardMemberRepositoryIntegrationTest {

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
    BoardMemberRepository boardMemberRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    BoardRepository boardRepository;
    @Test
    void isViewerOrOwner_shouldReturnTrueForBoardOwner() {
        // Test implementation here
        User user = new User();
        user.setUsername("integration1-user");
        user.setEmail("integration1@example.com");
        user.setPassword("password");

        user = userRepository.save(user);

        Board board = new Board();
        board.setName("Integration Board");
        board.setOwner(user);
        board = boardRepository.save(board);

        boolean result =
                boardMemberRepository.isViewerOrOwner(
                        board.getId(),
                        user.getId()
                );

        assertTrue(result);
    }

    @Test
    void isViewerOrOwner_shouldReturnTrueForMember() {

        User user = new User();
        user.setUsername("integration-user2");
        user.setEmail("integration2@example.com");
        user.setPassword("password");

        user = userRepository.save(user);

        Board board = new Board();
        board.setName("Integration Board");
        board.setOwner(user);
        board = boardRepository.save(board);

        BoardMember boardMember = new BoardMember();

        User member = new User();
        member.setUsername("integration-member");
        member.setEmail("lmember@gmail.com");
        member.setPassword("password");

        member = userRepository.save(member);

        boardMember.setBoard(board);
        boardMember.setUser(member);
        boardMember.setPermission(BoardPermission.VIEWER);
        boardMember = boardMemberRepository.save(boardMember);
        boolean result =
                boardMemberRepository.isViewerOrOwner(
                        board.getId(),
                        member.getId()
                );
        assertTrue(result);

    }
    @Test
    void isViewerOrOwner_shouldReturnFalseForRandomUser() {
        User user = new User();
        user.setUsername("integration-user3");
        user.setEmail("integration3@example.com");
        user.setPassword("password");

        user = userRepository.save(user);

        Board board = new Board();
        board.setName("Integration Board");
        board.setOwner(user);
        board = boardRepository.save(board);

        User random = new User();
        random.setUsername("random-user");
        random.setEmail("random@example.com");
        random.setPassword("password");
        random = userRepository.save(random);
        boolean result =
                boardMemberRepository.isViewerOrOwner(
                        board.getId(),
                        random.getId()
                );

        assertFalse(result);
    }
    @Test
    void isEditorOrOwner_shouldReturnTrueForOwner() {
        User owner = createUser("editor-owner", "editor-owner@test.com");

        Board board = new Board();
        board.setName("Board");
        board.setOwner(owner);
        board = boardRepository.save(board);

        assertTrue(
                boardMemberRepository.isEditorOrOwner(board.getId(), owner.getId())
        );
    }

    @Test
    void isEditorOrOwner_shouldReturnTrueForEditor() {
        User owner = createUser("editor-board-owner", "editor-board-owner@test.com");
        User editor = createUser("editor-member", "editor-member@test.com");

        Board board = new Board();
        board.setName("Board");
        board.setOwner(owner);
        board = boardRepository.save(board);

        BoardMember member = new BoardMember();
        member.setBoard(board);
        member.setUser(editor);
        member.setPermission(BoardPermission.EDITOR);
        boardMemberRepository.save(member);

        assertTrue(
                boardMemberRepository.isEditorOrOwner(board.getId(), editor.getId())
        );
    }

    @Test
    void isEditorOrOwner_shouldReturnFalseForViewer() {
        User owner = createUser("viewer-owner", "viewer-owner@test.com");
        User viewer = createUser("viewer-member", "viewer-member@test.com");

        Board board = new Board();
        board.setName("Board");
        board.setOwner(owner);
        board = boardRepository.save(board);

        BoardMember member = new BoardMember();
        member.setBoard(board);
        member.setUser(viewer);
        member.setPermission(BoardPermission.VIEWER);
        boardMemberRepository.save(member);

        assertFalse(
                boardMemberRepository.isEditorOrOwner(board.getId(), viewer.getId())
        );
    }

    @Test
    void isEditorOrOwner_shouldReturnFalseForRandomUser() {
        User owner = createUser("random-owner", "random-owner@test.com");
        User random = createUser("random-user", "random-user@test.com");

        Board board = new Board();
        board.setName("Board");
        board.setOwner(owner);
        board = boardRepository.save(board);

        assertFalse(
                boardMemberRepository.isEditorOrOwner(board.getId(), random.getId())
        );
    }
    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password");
        return userRepository.save(user);
    }
    @Test
    void findByBoardId_shouldReturnBoardMembers() {
        User owner = createUser("members-owner", "members-owner@test.com");
        User member = createUser("members-user", "members-user@test.com");

        Board board = new Board();
        board.setName("Board");
        board.setOwner(owner);
        board = boardRepository.save(board);

        BoardMember boardMember = new BoardMember();
        boardMember.setBoard(board);
        boardMember.setUser(member);
        boardMember.setPermission(BoardPermission.EDITOR);
        boardMemberRepository.save(boardMember);

        List<BoardMember> result =
                boardMemberRepository.findByBoardId(board.getId());

        assertEquals(1, result.size());
        assertEquals(member.getId(), result.get(0).getUser().getId());
    }
    @Test
    void findByUserId_shouldReturnUserMemberships() {
        User owner = createUser("user-owner", "user-owner@test.com");
        User member = createUser("user-member", "user-member@test.com");

        Board board = new Board();
        board.setName("Board");
        board.setOwner(owner);
        board = boardRepository.save(board);

        BoardMember boardMember = new BoardMember();
        boardMember.setBoard(board);
        boardMember.setUser(member);
        boardMember.setPermission(BoardPermission.VIEWER);
        boardMemberRepository.save(boardMember);

        List<BoardMember> result =
                boardMemberRepository.findByUserId(member.getId());

        assertEquals(1, result.size());
        assertEquals(board.getId(), result.get(0).getBoard().getId());
    }
    @Test
    void findByBoardIdAndUserId_shouldReturnMembership() {
        User owner = createUser("lookup-owner", "lookup-owner@test.com");
        User member = createUser("lookup-member", "lookup-member@test.com");

        Board board = new Board();
        board.setName("Board");
        board.setOwner(owner);
        board = boardRepository.save(board);

        BoardMember boardMember = new BoardMember();
        boardMember.setBoard(board);
        boardMember.setUser(member);
        boardMember.setPermission(BoardPermission.EDITOR);
        boardMemberRepository.save(boardMember);

        Optional<BoardMember> result =
                boardMemberRepository.findByBoardIdAndUserId(
                        board.getId(),
                        member.getId()
                );

        assertTrue(result.isPresent());
        assertEquals(
                BoardPermission.EDITOR,
                result.get().getPermission()
        );
    }
    @Test
    void findByBoardIdAndUserId_shouldReturnEmptyWhenMembershipDoesNotExist() {
        User owner = createUser("empty-owner", "empty-owner@test.com");
        User user = createUser("empty-user", "empty-user@test.com");

        Board board = new Board();
        board.setName("Board");
        board.setOwner(owner);
        board = boardRepository.save(board);

        Optional<BoardMember> result =
                boardMemberRepository.findByBoardIdAndUserId(
                        board.getId(),
                        user.getId()
                );

        assertTrue(result.isEmpty());
    }
    @BeforeEach
    void cleanDatabase() {
        boardMemberRepository.deleteAll();
        boardRepository.deleteAll();
        userRepository.deleteAll();
    }
}