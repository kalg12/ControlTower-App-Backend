-- ─────────────────────────────────────────────────────────────────────────────
-- V70: Mobile Push Tokens (Expo Push Service)
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE mobile_push_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       TEXT        NOT NULL,
    platform    VARCHAR(10) NOT NULL DEFAULT 'android',
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    device_info JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, token)
);

CREATE INDEX idx_push_tokens_user_active ON mobile_push_tokens (user_id, is_active);
