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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class BoardRepositoryIntegrationTest {

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
    BoardRepository boardRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    BoardMemberRepository boardMemberRepository;


    @Test
    void existsByOwnerIdAndId_shouldReturnTrueForBoardOwner() {

        User user = new User();
        user.setUsername("integration-user0");
        user.setEmail("integration0@example.com");
        user.setPassword("password");

        user = userRepository.save(user);

        Board board = new Board();
        board.setName("Integration Board");
        board.setOwner(user);

        board = boardRepository.save(board);

        boolean result =
                boardRepository.existsByOwnerIdAndId(
                        user.getId(),
                        board.getId()
                );

        assertTrue(result);
    }

    @Test
    void existsByOwnerIdAndId_shouldReturnFalseForNonOwner() {


        User owner = new User();
        owner.setUsername("integration-owner");
        owner.setEmail("owner@owner.com");
        owner.setPassword("password");
        owner = userRepository.save(owner);

        Board board = new Board();
        board.setName("Integration Board");
        board.setOwner(owner);

        board = boardRepository.save(board);
        boolean result =
                boardRepository.existsByOwnerIdAndId(
                        100L, // Non-owner user ID
                        board.getId()
                );
        assertFalse(result);
    }

    @Test
    void getAuthorizedBoard_shouldReturnBoardForOwner() {

        User owner = new User();
        owner.setUsername("integration-owner0");
        owner.setEmail("owne0r@example.com");
        owner.setPassword("password");

        owner = userRepository.save(owner);

        Board board = new Board();
        board.setName("Integration Board");
        board.setOwner(owner);

        board = boardRepository.save(board);

        Optional<Board> result =
                boardRepository.getAuthorizedBoard(
                        board.getId(),
                        owner.getId()
                );

        assertTrue(result.isPresent());
        assertEquals(board.getId(), result.get().getId());
    }
    @Test
    void getAuthorizedBoard_shouldReturnEmptyForUnauthorizedUser() {
        User owner = new User();
        owner.setUsername("integration-owner1");
        owner.setEmail("owner1@example.com");
        owner.setPassword("password");

        owner = userRepository.save(owner);

        Board board = new Board();
        board.setName("Integration Board");
        board.setOwner(owner);

        board = boardRepository.save(board);

        Optional<Board> result =
                boardRepository.getAuthorizedBoard(
                        board.getId(),
                        100L // Unauthorized user ID
                );
        assertFalse(result.isPresent());
    }
    @Test
    void getAuthorizedBoard_shouldReturnBoardForMember() {
        User owner = new User();
        owner.setUsername("integration-owner2");
        owner.setEmail("owner2@example.com");
        owner.setPassword("password");

        owner = userRepository.save(owner);

        Board board = new Board();
        board.setName("Integration Board");
        board.setOwner(owner);

        board = boardRepository.save(board);
       BoardMember boardMember = new BoardMember();
        User member = new User();
        member.setUsername("integration-member85");
        member.setEmail("lmembe5r@gmail.com");
        member.setPassword("password");
        member = userRepository.save(member);
        boardMember.setBoard(board);
        boardMember.setUser(member);
        boardMember.setPermission(BoardPermission.VIEWER);
        boardMember = boardMemberRepository.save(boardMember);
        Optional<Board> result =
                boardRepository.getAuthorizedBoard(
                        board.getId(),
                        member.getId() // Unauthorized user ID
                );
        assertTrue(result.isPresent());
        assertEquals(board.getId(), result.get().getId());
    }
    @BeforeEach
    void cleanDatabase() {
        boardMemberRepository.deleteAll();
        boardRepository.deleteAll();
        userRepository.deleteAll();
    }
}