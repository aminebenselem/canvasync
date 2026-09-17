package com.whiteboard.backend.element;


import com.whiteboard.backend.board.Board;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "elements")
@Getter
@Setter
@NoArgsConstructor
public class Element {
    @Id
    @GeneratedValue(strategy =GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ElementType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false )
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> data;
}
