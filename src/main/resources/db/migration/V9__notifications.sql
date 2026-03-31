-- ─────────────────────────────────────────────────────────────────────────────
-- V9: Notifications Hub
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE notifications (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    type        VARCHAR(100) NOT NULL,
    title       VARCHAR(255) NOT NULL,
    body        TEXT,
    severity    VARCHAR(20)  NOT NULL DEFAULT 'INFO',
    metadata    JSONB,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_tenant ON notifications (tenant_id, created_at DESC);
CREATE INDEX idx_notifications_type   ON notifications (tenant_id, type);

CREATE TABLE notification_user_states (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID        NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    read_at         TIMESTAMP WITH TIME ZONE,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    UNIQUE (notification_id, user_id)
);

CREATE INDEX idx_notif_user_states_user    ON notification_user_states (user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_notif_user_states_unread  ON notification_user_states (user_id, read_at) WHERE read_at IS NULL AND deleted_at IS NULL;
