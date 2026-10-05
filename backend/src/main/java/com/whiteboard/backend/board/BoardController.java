package com.whiteboard.backend.board;

import com.whiteboard.backend.board.dto.BoardDto;
import com.whiteboard.backend.board.dto.BoardMemberDto;
import com.whiteboard.backend.board.dto.CreateBoardDto;
import com.whiteboard.backend.board.dto.UpdateMemberPermissionDto;
import com.whiteboard.backend.board.exception.UnauthorizedUserException;
import com.whiteboard.backend.board.mapper.BoardMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/boards")
public class BoardController {

    private final BoardService boardService;
    private final BoardMapper boardMapper;

    public BoardController(
            BoardService boardService,
            BoardMapper boardMapper
    ) {
        this.boardService = boardService;
        this.boardMapper = boardMapper;

    }

    @PostMapping
    public BoardDto createBoard(
            @RequestBody CreateBoardDto dto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = getUserId(jwt);

        return boardMapper.toDto(
                boardService.createBoard(dto.name(), userId)
        );
    }

    @GetMapping("/{id}")
    public BoardDto getBoard(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = getUserId(jwt);

        return boardMapper.toDto(
                boardService.getBoard(id, userId)
        );
    }

    @GetMapping
    public List<BoardDto> getMyBoards(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = getUserId(jwt);

        return boardMapper.toDtoList(
                boardService.getUserBoards(userId)
        );
    }

    @GetMapping("/{id}/members")
    public List<BoardMemberDto> getMembers(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = getUserId(jwt);

        return boardService.listBoardMembers(id, userId);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public void deleteMember(
            @PathVariable UUID id,
            @PathVariable Long memberId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = getUserId(jwt);

        boardService.removeMember(id, userId, memberId);
    }

    @PatchMapping("/{id}/members")
    public void updateMember(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateMemberPermissionDto dto
    ) {
        Long userId = getUserId(jwt);

        boardService.updateMemberPermission(id, userId, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteBoard(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = getUserId(jwt);

        boardService.deleteBoard(id, userId);
    }
    @GetMapping("/{boardId}/can-edit")
    public boolean canEdit(
            @PathVariable UUID boardId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = getUserId(jwt);
        return boardService.canEdit(boardId, userId);
    }
    @GetMapping("/{boardId}/is-owner")
    public boolean isOwner(
            @PathVariable UUID boardId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = getUserId(jwt);
        return boardService.isOwner(boardId, userId);
    }

    private Long getUserId(Jwt jwt) {
        if( jwt == null || jwt.getSubject() == null) {
            throw new UnauthorizedUserException("JWT or subject is null");
        }
        return Long.parseLong(jwt.getSubject());
    }


}