-- V67: Make visitor_email NOT NULL on chat_conversations
-- Required to guarantee email notifications can always be sent to visitors.
-- Backfill any legacy rows that have no email with an empty string before adding constraint.
UPDATE chat_conversations SET visitor_email = '' WHERE visitor_email IS NULL;
ALTER TABLE chat_conversations ALTER COLUMN visitor_email SET NOT NULL;
