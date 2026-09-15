package com.whiteboard.backend.boardmember;

import com.whiteboard.backend.board.Board;
import com.whiteboard.backend.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "board_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_board_member",
                        columnNames = {"board_id", "user_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class BoardMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardPermission permission;
}