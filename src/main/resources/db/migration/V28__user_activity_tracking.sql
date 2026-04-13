-- ─────────────────────────────────────────────────────────────────────────────
-- V28: User Activity Tracking
-- Tracks page navigation and employee activity within Control Tower.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS user_activity (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    user_id             UUID NOT NULL,
    user_name           VARCHAR(255),
    user_email          VARCHAR(255),
    session_id          VARCHAR(255),
    route_path          VARCHAR(500) NOT NULL,
    page_title          VARCHAR(255),
    duration_seconds    INTEGER,
    full_url            VARCHAR(1000),
    user_agent          TEXT,
    ip_address          VARCHAR(64),
    visited_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_activity_user_time ON user_activity(user_id, visited_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_activity_session ON user_activity(session_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_activity_route ON user_activity(route_path) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_activity_visited ON user_activity(visited_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_activity_tenant_time ON user_activity(tenant_id, visited_at DESC) WHERE deleted_at IS NULL;
