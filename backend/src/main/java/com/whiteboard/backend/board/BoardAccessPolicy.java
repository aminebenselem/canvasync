package com.whiteboard.backend.board;


import com.whiteboard.backend.board.boardmember.BoardMemberRepository;
import com.whiteboard.backend.board.exception.BoardAccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BoardAccessPolicy {

    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;

    public BoardAccessPolicy(
            BoardRepository boardRepository,
            BoardMemberRepository boardMemberRepository
    ) {
        this.boardRepository = boardRepository;
        this.boardMemberRepository = boardMemberRepository;
    }

    public boolean isOwner(UUID boardId, Long userId) {
        return boardRepository.existsByOwnerIdAndId(userId, boardId);
    }
    public Board getBoardIfAuthorized(UUID boardId, Long userId) {
        return boardRepository.getAuthorizedBoard(boardId, userId)
                .orElseThrow(() -> new BoardAccessDeniedException("Access denied to the board"));
    }
    public boolean canView(UUID boardId, Long userId) {
  return boardMemberRepository.isViewerOrOwner(boardId, userId);
    }
  public  boolean canEdit(UUID boardId, Long userId) {
        return boardMemberRepository.isEditorOrOwner(boardId, userId);
    }





}
