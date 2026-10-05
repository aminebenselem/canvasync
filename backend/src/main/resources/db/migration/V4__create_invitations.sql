CREATE TABLE invitations (
                             id UUID PRIMARY KEY,
                             board_id UUID NOT NULL,
                             user_id BIGINT NOT NULL,
                             status VARCHAR(20) NOT NULL,
                             sender_id BIGINT NOT NULL,
                             permission VARCHAR(30) NOT NULL

                              CONSTRAINT chk_invitation_permission
                                 CHECK (permission IN ('VIEWER', 'EDITOR')),

                             CONSTRAINT fk_invitation_sender
                                 FOREIGN KEY (sender_id)
                                     REFERENCES users(id)
                                 ON DELETE CASCADE,
                             CONSTRAINT fk_invitation_board
                                 FOREIGN KEY (board_id)
                                     REFERENCES boards(id)
                                     ON DELETE CASCADE,
                             CONSTRAINT fk_invitation_user
                                 FOREIGN KEY (user_id)
                                     REFERENCES users(id)
                                     ON DELETE CASCADE,

                             CONSTRAINT chk_invitation_status
                                 CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED'))
);

CREATE UNIQUE INDEX uq_pending_invitation
    ON invitations (board_id, user_id)
    WHERE status = 'PENDING';