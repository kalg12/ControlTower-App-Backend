-- ─────────────────────────────────────────────────────────────────────────────
-- V19: User settings — key/value store per user for notification preferences
--      and other per-user configuration.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE user_settings (
    id         UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id    UUID         NOT NULL,
    key        VARCHAR(100) NOT NULL,
    value      TEXT         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_id, key)
);

CREATE INDEX idx_user_settings_user ON user_settings (user_id);
