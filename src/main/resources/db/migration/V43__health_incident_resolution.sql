-- Add resolution fields to health_incidents table
ALTER TABLE health_incidents ADD COLUMN resolved_by UUID REFERENCES users(id);
ALTER TABLE health_incidents ADD COLUMN resolution_note TEXT;