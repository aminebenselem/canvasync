package com.whiteboard.backend.board;


import com.whiteboard.backend.board.dto.CreateBoardDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @PostMapping
    public CreateBoardDto createBoard(
            @RequestBody CreateBoardDto dto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());

        return boardService.createBoard(dto.name(), userId);
    }

    @GetMapping("/{id}")
    public BoardDto getBoard(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());

        return boardService.getBoard(id, userId);
    }

    @GetMapping
    public List<BoardDto> getMyBoards(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());

        return boardService.getUserBoards(userId);
    }
}