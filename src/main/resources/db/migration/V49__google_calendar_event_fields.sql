-- Add Google Calendar sync field to calendar events
ALTER TABLE calendar_events
ADD COLUMN IF NOT EXISTS google_event_id VARCHAR(100);