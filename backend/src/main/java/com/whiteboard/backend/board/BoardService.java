package com.whiteboard.backend.board;

import com.whiteboard.backend.board.boardmember.BoardMember;
import com.whiteboard.backend.board.boardmember.BoardMemberRepository;
import com.whiteboard.backend.board.dto.AddMemberDto;
import com.whiteboard.backend.board.dto.UpdateMemberPermissionDto;
import com.whiteboard.backend.board.exception.BoardAccessDeniedException;
import com.whiteboard.backend.board.exception.InvalidMembershipException;
import com.whiteboard.backend.user.User;
import com.whiteboard.backend.user.UserService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final UserService userService;
    private final BoardAccessPolicy boardAccessPolicy;

    public BoardService(
            BoardRepository boardRepository,
            BoardMemberRepository boardMemberRepository,
            UserService userService,
            BoardAccessPolicy boardAccessPolicy
    ) {
        this.boardRepository = boardRepository;
        this.boardMemberRepository = boardMemberRepository;
        this.userService = userService;
        this.boardAccessPolicy = boardAccessPolicy;
    }

    public Board createBoard(String name, Long ownerId) {
        User owner = userService.getUserEntity(ownerId);

        Board board = new Board();
        board.setName(name);
        board.setOwner(owner);

        return boardRepository.save(board);
    }

    public Board getBoard(UUID boardId, Long userId) {
        return boardAccessPolicy.getBoardIfAuthorized(boardId, userId);
    }

    public List<Board> getUserBoards(Long userId) {
        List<Board> boards = new ArrayList<>();
        boards.addAll(boardRepository.findByOwnerId(userId));
        boards.addAll(boardMemberRepository.findBoardsByUserId(userId));
        return boards;
    }

    @Transactional
    public void addMember(
            UUID boardId,
            Long userId,
            AddMemberDto addMemberDto
    ) {
        if (Objects.equals(addMemberDto.memberId(), userId)) {
            throw new InvalidMembershipException("Invalid membership");
        }

        if (!boardAccessPolicy.isOwner(boardId, userId)) {
            throw new BoardAccessDeniedException(
                    "Only the owner can add members"
            );
        }

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() ->
                        new InvalidMembershipException("Board not found"));

        User newMember = userService.getUserEntity(addMemberDto.memberId());

        BoardMember boardMember = new BoardMember();
        boardMember.setBoard(board);
        boardMember.setPermission(addMemberDto.permission());
        boardMember.setUser(newMember);

        boardMemberRepository.save(boardMember);
    }

    public void removeMember(
            UUID boardId,
            Long ownerId,
            Long memberId
    ) {
        if (!boardAccessPolicy.isOwner(boardId, ownerId)) {
            throw new BoardAccessDeniedException(
                    "Only the owner can remove members"
            );
        }

        boardMemberRepository.deleteByBoardIdAndUserId(
                boardId,
                memberId
        );
    }

    public void updateMemberPermission(
            UUID boardId,
            Long ownerId,
            UpdateMemberPermissionDto dto
    ) {
        if (!boardAccessPolicy.isOwner(boardId, ownerId)) {
            throw new BoardAccessDeniedException(
                    "Only the owner can update member permissions"
            );
        }

        BoardMember boardMember =
                boardMemberRepository
                        .findByBoardIdAndUserId(
                                boardId,
                                dto.memberId()
                        )
                        .orElseThrow(() ->
                                new InvalidMembershipException(
                                        "Member not found in the board"
                                ));

        boardMember.setPermission(dto.permission());
        boardMemberRepository.save(boardMember);
    }

    @Transactional
    public void deleteBoard(UUID boardId, Long userId) {
        if (!boardAccessPolicy.isOwner(boardId, userId)) {
            throw new BoardAccessDeniedException(
                    "Only the owner can delete the board"
            );
        }

        boardRepository.deleteById(boardId);
    }

    public List<User> listBoardMembers(
            UUID boardId,
            Long userId
    ) {
        if (!boardAccessPolicy.isOwner(boardId, userId)) {
            throw new BoardAccessDeniedException(
                    "Only the owner can list members"
            );
        }

        return boardMemberRepository.findUsersByBoardId(boardId);
    }
}