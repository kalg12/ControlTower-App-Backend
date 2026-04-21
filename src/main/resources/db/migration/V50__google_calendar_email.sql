-- Add Google Calendar email field to users
ALTER TABLE users
ADD COLUMN IF NOT EXISTS google_calendar_email VARCHAR(255);