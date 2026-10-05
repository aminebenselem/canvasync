package com.whiteboard.backend.board;

import com.whiteboard.backend.board.access.BoardAccessPolicy;
import com.whiteboard.backend.board.boardmember.BoardMember;
import com.whiteboard.backend.board.boardmember.BoardMemberRepository;
import com.whiteboard.backend.board.boardmember.BoardPermission;
import com.whiteboard.backend.board.dto.AddMemberDto;
import com.whiteboard.backend.board.dto.BoardMemberDto;
import com.whiteboard.backend.board.dto.UpdateMemberPermissionDto;
import com.whiteboard.backend.board.exception.BoardAccessDeniedException;
import com.whiteboard.backend.board.exception.InvalidMembershipException;
import com.whiteboard.backend.board.mapper.BoardMemberMapper;
import com.whiteboard.backend.user.User;
import com.whiteboard.backend.user.UserService;
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
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardMemberRepository boardMemberRepository;

    @Mock
    private UserService userService;

    @Mock
    private BoardAccessPolicy boardAccessPolicy;

    @Mock
    private BoardMemberMapper boardMemberMapper;

    @InjectMocks
    private BoardService boardService;

    @Test
    void createBoard_shouldCreateBoardForOwner() {

        // Arrange
        Long ownerId = 1L;
        String boardName = "My Whiteboard";

        User owner = new User();

        when(userService.getUserEntity(ownerId))
                .thenReturn(owner);

        Board savedBoard = new Board();
        savedBoard.setName(boardName);
        savedBoard.setOwner(owner);

        when(boardRepository.save(any(Board.class)))
                .thenReturn(savedBoard);


        // Act
        Board result = boardService.createBoard(
                boardName,
                ownerId
        );


        // Assert
        assertNotNull(result);
        assertEquals(boardName, result.getName());
        assertEquals(owner, result.getOwner());

        verify(userService)
                .getUserEntity(ownerId);

        verify(boardRepository)
                .save(any(Board.class));
    }

    @Test
    void getBoard_shouldThrowWhenUserIsUnauthorized() {

        // Arrange

        UUID boardId = UUID.randomUUID();
        Long userId = 2L;

        when(boardAccessPolicy.getBoardIfAuthorized(boardId, userId))
                .thenThrow(
                        new BoardAccessDeniedException(
                                "Access denied"
                        )
                );
        // Act + Assert
        assertThrows(
                BoardAccessDeniedException.class,
                () -> boardService.getBoard(boardId, userId)
        );
        // Verify
        verify(boardAccessPolicy)
                .getBoardIfAuthorized(boardId, userId);
    }

    @Test
    void removeMember_shouldThrowWhenUserIsNotOwner() {

        // Arrange

        UUID boardId = UUID.randomUUID();
        Long userId = 2L;
        Long memberId = 5L;

        when(boardAccessPolicy.isOwner(boardId, userId))
                .thenReturn(false);

        assertThrows(
                BoardAccessDeniedException.class,
                () -> boardService.removeMember(boardId, userId, memberId));


        verify(boardAccessPolicy)
                .isOwner(boardId, userId);

        verify(boardMemberRepository, never())
                .deleteByBoardIdAndUserId(
                        boardId,
                        memberId
                );

    }

    @Test
    void removeMember_shouldDeleteMemberWhenUserIsOwner() {


        UUID boardId = UUID.randomUUID();
        Long ownerId = 1L;
        Long memberId = 5L;

        when(boardAccessPolicy.isOwner(boardId, ownerId))
                .thenReturn(true);

        boardService.removeMember(
                boardId,
                ownerId,
                memberId
        );

        verify(boardAccessPolicy)
                .isOwner(boardId, ownerId);

        verify(boardMemberRepository)
                .deleteByBoardIdAndUserId(
                        boardId,
                        memberId
                );
    }

    @Test
    void updateMemberPermission_shouldThrowWhenMemberDoesNotExist() {

        // Arrange

        UUID boardId = UUID.randomUUID();
        Long ownerId = 1L;
        Long memberId = 5L;

        UpdateMemberPermissionDto dto =
                new UpdateMemberPermissionDto(
                        memberId,
                        BoardPermission.EDITOR
                );

        when(boardAccessPolicy.isOwner(boardId, ownerId))
                .thenReturn(true);

        when(boardMemberRepository.findByBoardIdAndUserId(
                boardId,
                memberId
        )).thenReturn(Optional.empty());


        // Act + Assert

        assertThrows(
                InvalidMembershipException.class,
                () -> boardService.updateMemberPermission(
                        boardId,
                        ownerId,
                        dto
                )
        );


        // Verify

        verify(boardMemberRepository)
                .findByBoardIdAndUserId(
                        boardId,
                        memberId
                );

        verify(boardMemberRepository, never())
                .save(any(BoardMember.class));
    }

    @Test
    void updateMemberPermission_shouldUpdatePermission() {
        UUID boardId = UUID.randomUUID();
        Long ownerId = 1L;
        Long memberId = 5L;
        BoardPermission permission = BoardPermission.VIEWER;
        UpdateMemberPermissionDto dto =
                new UpdateMemberPermissionDto(
                        memberId,
                        BoardPermission.EDITOR
                );
        when(boardAccessPolicy.isOwner(boardId, ownerId))
                .thenReturn(true);
        User user = new User();
        user.setId(ownerId);
        BoardMember boardMember = new BoardMember();
        boardMember.setBoard(new Board());
        boardMember.setUser(user);
        boardMember.setPermission(permission);

        when(boardMemberRepository.findByBoardIdAndUserId(
                boardId,
                memberId
        )).thenReturn(Optional.of(boardMember));

        // Act
        boardService.updateMemberPermission(
                boardId,
                ownerId,
                dto
        );

        // Assert
        assertEquals(BoardPermission.EDITOR, boardMember.getPermission());

        verify(boardMemberRepository)
                .findByBoardIdAndUserId(
                        boardId,
                        memberId
                );

        verify(boardMemberRepository)
                .save(boardMember);


    }

    @Test
    void addMember_shouldThrowWhenUserAddsThemselves() {
        UUID boardId = UUID.randomUUID();
        Long ownerId = 1L;
        Long memberId = 1L;
        AddMemberDto dto = new AddMemberDto(
                memberId,
                BoardPermission.EDITOR
        );

        assertThrows(InvalidMembershipException.class, () -> boardService.addMember(boardId, ownerId, dto));
    }

    @Test
    void addMember_shouldThrowWhenUserIsNotOwner() {
        UUID boardId = UUID.randomUUID();
        Long ownerId = 1L;
        Long memberId = 2L;
        AddMemberDto dto = new AddMemberDto(
                memberId,
                BoardPermission.EDITOR
        );
        when(boardAccessPolicy.isOwner(boardId, ownerId)).thenReturn(false);
        assertThrows(BoardAccessDeniedException.class, () -> boardService.addMember(boardId, ownerId, dto));

        verify(boardAccessPolicy)
                .isOwner(boardId, ownerId);
        verify(boardMemberRepository, never())
                .save(any(BoardMember.class));
    }

    @Test
    void addMember_shouldAddMemberWhenUserIsOwner() {
        UUID boardId = UUID.randomUUID();
        Long ownerId = 1L;
        Long memberId = 2L;
        AddMemberDto dto = new AddMemberDto(
                memberId,
                BoardPermission.EDITOR
        );


        when(boardAccessPolicy.isOwner(boardId, ownerId)).thenReturn(true);



        when(boardRepository.findById(boardId)).thenReturn(Optional.of(new Board()));
        when(userService.getUserEntity(memberId)).thenReturn(new User());
        when(boardMemberRepository.save(any(BoardMember.class))).thenReturn(null);

        boardService.addMember(boardId, ownerId, dto);

        verify(boardAccessPolicy)
                .isOwner(boardId, ownerId);
        verify(boardRepository).findById(boardId);
        verify(userService).getUserEntity(memberId);
        verify(boardMemberRepository).save(any(BoardMember.class));
    }
    @Test
    void getUserBoards_shouldReturnOwnedAndMemberBoards() {
        Long userId = 1L;

        Board ownedBoard = new Board();
        ownedBoard.setName("Owned Board");

        Board memberBoard = new Board();
        memberBoard.setName("Member Board");

        BoardMember membership = new BoardMember();
        membership.setBoard(memberBoard);

        when(boardRepository.findByOwnerId(userId))
                .thenReturn(List.of(ownedBoard));

        when(boardMemberRepository.findByUserId(userId))
                .thenReturn(List.of(membership));

        List<Board> result =
                boardService.getUserBoards(userId);

        assertEquals(2, result.size());
        assertTrue(result.contains(ownedBoard));
        assertTrue(result.contains(memberBoard));

        verify(boardRepository)
                .findByOwnerId(userId);

        verify(boardMemberRepository)
                .findByUserId(userId);
    }
    @Test
    void listBoardMembers_shouldReturnMembersWhenUserCanViewBoard() {
        UUID boardId = UUID.randomUUID();
        Long userId = 2L;

        BoardMember member = new BoardMember();

        List<BoardMember> members = List.of(member);

        BoardMemberDto dto = new BoardMemberDto(
                2L,
                "amine",
                "amine@example.com",
                BoardPermission.EDITOR
        );

        List<BoardMemberDto> expectedDtos = List.of(dto);

        when(boardAccessPolicy.canView(boardId, userId))
                .thenReturn(true);

        when(boardMemberRepository.findByBoardId(boardId))
                .thenReturn(members);

        when(boardMemberMapper.toDtoList(members))
                .thenReturn(expectedDtos);

        List<BoardMemberDto> result =
                boardService.listBoardMembers(boardId, userId);

        assertEquals(expectedDtos, result);

        verify(boardAccessPolicy)
                .canView(boardId, userId);

        verify(boardMemberRepository)
                .findByBoardId(boardId);

        verify(boardMemberMapper)
                .toDtoList(members);
    }
    @Test
    void listBoardMembers_shouldThrowWhenUserCannotViewBoard() {
        UUID boardId = UUID.randomUUID();
        Long userId = 2L;

        when(boardAccessPolicy.canView(boardId, userId))
                .thenReturn(false);

        assertThrows(
                BoardAccessDeniedException.class,
                () -> boardService.listBoardMembers(boardId, userId)
        );

        verify(boardAccessPolicy)
                .canView(boardId, userId);

        verify(boardMemberRepository, never())
                .findByBoardId(boardId);
    }
    @Test
    void deleteBoard_shouldDeleteBoardWhenUserIsOwner() {
        UUID boardId = UUID.randomUUID();
        Long userId = 1L;

        when(boardAccessPolicy.isOwner(boardId, userId))
                .thenReturn(true);

        boardService.deleteBoard(boardId, userId);

        verify(boardAccessPolicy)
                .isOwner(boardId, userId);

        verify(boardRepository)
                .deleteById(boardId);
    }
    @Test
    void deleteBoard_shouldThrowWhenUserIsNotOwner() {
        UUID boardId = UUID.randomUUID();
        Long userId = 2L;

        when(boardAccessPolicy.isOwner(boardId, userId))
                .thenReturn(false);

        assertThrows(
                BoardAccessDeniedException.class,
                () -> boardService.deleteBoard(boardId, userId)
        );

        verify(boardAccessPolicy)
                .isOwner(boardId, userId);

        verify(boardRepository, never())
                .deleteById(boardId);
    }
}