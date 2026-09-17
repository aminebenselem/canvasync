package com.whiteboard.backend.board;

import com.whiteboard.backend.board.dto.AddMemberDto;
import com.whiteboard.backend.board.dto.BoardDto;
import com.whiteboard.backend.board.dto.CreateBoardDto;
import com.whiteboard.backend.board.dto.UpdateMemberPermissionDto;
import com.whiteboard.backend.board.mapper.BoardMapper;
import com.whiteboard.backend.user.dto.UserDto;
import com.whiteboard.backend.user.mapper.UserMapper;
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
    private final UserMapper userMapper;

    public BoardController(
            BoardService boardService,
            BoardMapper boardMapper,
            UserMapper userMapper
    ) {
        this.boardService = boardService;
        this.boardMapper = boardMapper;
        this.userMapper = userMapper;
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

    @PostMapping("/{id}/members")
    public void addMember(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody AddMemberDto dto
    ) {
        Long userId = getUserId(jwt);

        boardService.addMember(id, userId, dto);
    }

    @GetMapping("/{id}/members")
    public List<UserDto> getMembers(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = getUserId(jwt);

        return userMapper.toDtoList(
                boardService.listBoardMembers(id, userId)
        );
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

    private Long getUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}