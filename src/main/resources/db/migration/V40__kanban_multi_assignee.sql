-- ─────────────────────────────────────────────────────────────────────────────
-- V40: Kanban — replace single assignee_id with multi-assignee join table
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE card_assignees (
    card_id UUID NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (card_id, user_id)
);

CREATE INDEX idx_card_assignees_user ON card_assignees (user_id);

-- Migrate existing single assignee to the join table
INSERT INTO card_assignees (card_id, user_id)
SELECT id, assignee_id
FROM   cards
WHERE  assignee_id IS NOT NULL
  AND  deleted_at  IS NULL;

ALTER TABLE cards DROP COLUMN IF EXISTS assignee_id;
