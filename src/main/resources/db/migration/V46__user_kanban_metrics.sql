-- Add kanban metrics to users
ALTER TABLE users ADD COLUMN kanban_points INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN overdue_attended INTEGER NOT NULL DEFAULT 0;