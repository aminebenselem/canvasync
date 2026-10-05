package com.whiteboard.backend.board.mapper;

import com.whiteboard.backend.board.Board;
import com.whiteboard.backend.board.boardmember.BoardMember;
import com.whiteboard.backend.board.dto.BoardDto;
import com.whiteboard.backend.board.dto.BoardMemberDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BoardMemberMapper {
    public BoardMemberDto toDto(BoardMember boardMember) {

        return new BoardMemberDto(
                boardMember.getUser().getId(),
                boardMember.getUser().getUsername(),
                boardMember.getUser().getEmail(),
                boardMember.getPermission()
                );

    }

    public List<BoardMemberDto> toDtoList(List<BoardMember> boards) {
        return boards.stream()
                .map(this::toDto)
                .toList();
    }
}