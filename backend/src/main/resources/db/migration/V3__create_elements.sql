CREATE TABLE elements (
                          id UUID PRIMARY KEY,
                          type VARCHAR(50) NOT NULL,
                          board_id UUID NOT NULL,
                          data JSONB NOT NULL,

                          CONSTRAINT fk_elements_board
                              FOREIGN KEY (board_id)
                                  REFERENCES boards(id)
                                  ON DELETE CASCADE
);

CREATE INDEX idx_elements_board_id
    ON elements(board_id);