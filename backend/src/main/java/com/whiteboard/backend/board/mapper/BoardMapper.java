package com.whiteboard.backend.board.mapper;

import com.whiteboard.backend.board.Board;
import com.whiteboard.backend.board.dto.BoardDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BoardMapper {

    public BoardDto toDto(Board board) {

        return new BoardDto(
                board.getId(),
                board.getName(),
                board.getOwner().getId(),
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
    }
    public List<BoardDto> toDtoList(List<Board> boards) {
        return boards.stream()
                .map(this::toDto)
                .toList();
    }
}

