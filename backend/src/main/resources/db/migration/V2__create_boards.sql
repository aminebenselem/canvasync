CREATE TABLE boards (
                        id UUID PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        owner_id BIGINT NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT fk_board_owner
                            FOREIGN KEY (owner_id)
                                REFERENCES users(id)
);

CREATE TABLE board_members (
                               id UUID PRIMARY KEY,
                               board_id UUID NOT NULL,
                               user_id BIGINT NOT NULL,
                               permission VARCHAR(20) NOT NULL,

                               CONSTRAINT fk_board_member_board
                                   FOREIGN KEY (board_id)
                                       REFERENCES boards(id)
                                       ON DELETE CASCADE,

                               CONSTRAINT fk_board_member_user
                                   FOREIGN KEY (user_id)
                                       REFERENCES users(id)
                                       ON DELETE CASCADE,

                               CONSTRAINT uk_board_member
                                   UNIQUE (board_id, user_id),

                               CONSTRAINT chk_board_member_permission
                                   CHECK (permission IN ('VIEWER', 'EDITOR'))
);