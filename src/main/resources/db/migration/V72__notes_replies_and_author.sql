-- ── Notes: add reply threading support ───────────────────────────────────────
-- parent_id points to the root note; replies are always one level deep.
ALTER TABLE notes ADD COLUMN IF NOT EXISTS parent_id UUID REFERENCES notes(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_notes_parent ON notes (parent_id) WHERE parent_id IS NOT NULL;
