ALTER TABLE chat_messages
    ADD COLUMN internal BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_chat_messages_internal
    ON chat_messages (conversation_id, internal, created_at);
