package com.whiteboard.backend.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardRepository extends JpaRepository<Board, UUID> {

    List<Board> findByOwnerId(Long ownerId);

    @Query("""
    SELECT b
    FROM Board b
    LEFT JOIN BoardMember bm ON b.id = bm.board.id
    WHERE b.id = :boardId
      AND (b.owner.id = :userId OR bm.user.id = :userId)
""")
    Optional<Board> getAuthorizedBoard(
            @Param("boardId") UUID boardId,
            @Param("userId") Long userId
    );
boolean existsByOwnerIdAndId(Long ownerId, UUID boardId);
}