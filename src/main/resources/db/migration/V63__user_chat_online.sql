-- V63: Agent presence for live chat (online/offline toggle)
ALTER TABLE users ADD COLUMN IF NOT EXISTS chat_online BOOLEAN NOT NULL DEFAULT FALSE;
