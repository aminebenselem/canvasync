package com.whiteboard.backend.auth.invitation;

import com.whiteboard.backend.board.Board;
import com.whiteboard.backend.board.boardmember.BoardPermission;
import com.whiteboard.backend.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "invitations" )
@Getter
@Setter
@NoArgsConstructor
public class Invitation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;
    @ManyToOne(fetch =FetchType.LAZY,optional = false)
    @JoinColumn(name ="user_id")
    private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardPermission permission;
}
