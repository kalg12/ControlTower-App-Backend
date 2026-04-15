-- ─────────────────────────────────────────────────────────────────────────────
-- V31: Time Tracking — time_entries table + estimated_minutes on tickets/cards
-- ─────────────────────────────────────────────────────────────────────────────

-- Estimated time fields
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS estimated_minutes INTEGER;
ALTER TABLE cards   ADD COLUMN IF NOT EXISTS estimated_minutes INTEGER;

-- Time entries (work log for tickets and kanban cards)
CREATE TABLE time_entries (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id       UUID        NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
    entity_type   VARCHAR(10) NOT NULL CHECK (entity_type IN ('TICKET', 'CARD')),
    entity_id     UUID        NOT NULL,
    started_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at      TIMESTAMP WITH TIME ZONE,
    minutes       INTEGER,
    note          TEXT,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_time_entries_tenant       ON time_entries (tenant_id)                    WHERE deleted_at IS NULL;
CREATE INDEX idx_time_entries_user         ON time_entries (user_id)                      WHERE deleted_at IS NULL;
CREATE INDEX idx_time_entries_entity       ON time_entries (entity_type, entity_id)       WHERE deleted_at IS NULL;
CREATE INDEX idx_time_entries_active       ON time_entries (tenant_id, user_id, ended_at) WHERE deleted_at IS NULL AND ended_at IS NULL;

-- SLA notification deduplication table (avoid sending duplicate SLA warnings)
CREATE TABLE sla_notifications_sent (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id  UUID        NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    threshold  SMALLINT    NOT NULL CHECK (threshold IN (50, 75, 90)),
    sent_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (ticket_id, threshold)
);
