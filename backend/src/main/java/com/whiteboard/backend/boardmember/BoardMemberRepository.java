package com.whiteboard.backend.boardmember;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardMemberRepository
        extends JpaRepository<BoardMember, UUID> {

    Optional<BoardMember> findByBoardIdAndUserId(
            UUID boardId,
            Long userId
    );

    boolean existsByBoardIdAndUserId(
            UUID boardId,
            Long userId
    );
}