-- V25: Add POS context JSONB column to tickets and source index
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS pos_context JSONB;

CREATE INDEX IF NOT EXISTS idx_tickets_source ON tickets (source) WHERE deleted_at IS NULL;
