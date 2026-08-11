ALTER TABLE integration_endpoints
    ADD COLUMN IF NOT EXISTS webhook_secret VARCHAR(512);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS chat_online_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_users_chat_online_at
    ON users (tenant_id, chat_online_at)
    WHERE deleted_at IS NULL AND chat_online = TRUE;
