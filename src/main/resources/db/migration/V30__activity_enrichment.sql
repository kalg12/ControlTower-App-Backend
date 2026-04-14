-- ─────────────────────────────────────────────────────────────────────────────
-- V30: Activity Feed enrichment
-- Adds action-event tracking columns to user_activity so the table stores
-- both page-navigation events (event_type='NAVIGATION', existing rows) and
-- business-action events (event_type='ACTION', emitted by backend services).
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE user_activity
    ADD COLUMN IF NOT EXISTS event_type   VARCHAR(20)  NOT NULL DEFAULT 'NAVIGATION',
    ADD COLUMN IF NOT EXISTS action_name  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS entity_type  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS entity_id    VARCHAR(255),
    ADD COLUMN IF NOT EXISTS description  TEXT,
    ADD COLUMN IF NOT EXISTS metadata     JSONB;

-- Composite index for the most common filtered query (tenant + event_type + time)
CREATE INDEX IF NOT EXISTS idx_activity_event_type
    ON user_activity(event_type) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_activity_tenant_type_time
    ON user_activity(tenant_id, event_type, visited_at DESC) WHERE deleted_at IS NULL;
