package com.whiteboard.backend.board.boardmember;

import com.whiteboard.backend.board.Board;
import com.whiteboard.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardMemberRepository
        extends JpaRepository<BoardMember, UUID> {


    List<Board> findBoardsByUserId(Long userId);

    Optional<BoardMember> findByBoardIdAndUserId(
            UUID boardId,
            Long userId
    );
    void deleteByBoardIdAndUserId(UUID boardId, Long userId);
    @Query("""
                  SELECT  u
                 FROM User u
                 JOIN BoardMember bm  ON  bm.user.id = u.id
                 WHERE bm.board.id = :boardId""")

    List<User> findUsersByBoardId(@Param("boardId") UUID boardId);


    @Query("""
            SELECT CASE WHEN COUNT(b) > 0   THEN true ELSE false END
            FROM Board b
            LEFT JOIN BoardMember bm ON b.id = bm.board.id
            WHERE b.id = :boardId AND (b.owner.id = :userId OR bm.user.id = :userId)
        """)
    boolean isViewerOrOwner(
            @Param("boardId") UUID boardId,
            @Param("userId") Long userId
    );
    @Query("""
            SELECT CASE WHEN COUNT(b) > 0   THEN true ELSE false END
            FROM Board b
            LEFT JOIN BoardMember bm ON b.id = bm.board.id
            WHERE b.id = :boardId AND ((b.owner.id = :userId) OR (bm.user.id = :userId AND bm.permission = 'EDITOR' ))
        """)
    boolean isEditorOrOwner(
            @Param("boardId") UUID boardId,
            @Param("userId") Long userId
    );

}


