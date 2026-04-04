-- Optional workflow kind for columns (default boards + cross-board filters). Custom columns stay NULL.
ALTER TABLE board_columns ADD COLUMN column_kind VARCHAR(32);

CREATE INDEX idx_board_columns_kind ON board_columns (column_kind) WHERE column_kind IS NOT NULL;
