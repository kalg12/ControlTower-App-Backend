-- Add attendance tracking to cards for overdue task management
ALTER TABLE cards ADD COLUMN attended_by UUID REFERENCES users(id);
ALTER TABLE cards ADD COLUMN attended_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE cards ADD COLUMN was_overdue BOOLEAN DEFAULT FALSE;