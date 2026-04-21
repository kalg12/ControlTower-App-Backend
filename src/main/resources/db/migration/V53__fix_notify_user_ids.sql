-- Fix notify_user_ids column type for Hibernate 7 compatibility
-- Drop the incorrectly typed column and recreate as JSONB
ALTER TABLE client_reminders DROP COLUMN IF EXISTS notify_user_ids;

ALTER TABLE client_reminders ADD COLUMN notify_user_ids JSONB DEFAULT '[]';
