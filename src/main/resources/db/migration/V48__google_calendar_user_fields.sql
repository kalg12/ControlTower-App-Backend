-- Add Google Calendar integration fields to users
ALTER TABLE users
ADD COLUMN IF NOT EXISTS google_refresh_token TEXT,
ADD COLUMN IF NOT EXISTS google_calendar_enabled BOOLEAN NOT NULL DEFAULT FALSE;