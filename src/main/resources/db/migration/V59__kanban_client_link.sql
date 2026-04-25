-- ─────────────────────────────────────────────────────────────────────────────
-- V59: Link Kanban boards and cards to CRM clients
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE boards ADD COLUMN client_id UUID REFERENCES clients(id);
ALTER TABLE cards  ADD COLUMN client_id UUID REFERENCES clients(id);

CREATE INDEX idx_boards_client    ON boards (client_id) WHERE client_id IS NOT NULL;
CREATE INDEX idx_cards_client    ON cards  (client_id) WHERE client_id IS NOT NULL;