-- Client Reminders table
CREATE TABLE client_reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    client_id UUID NOT NULL REFERENCES clients(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    
    -- Recurrence
    recurrence_type VARCHAR(20) NOT NULL DEFAULT 'WEEKLY',
    recurrence_days INTEGER,
    start_date TIMESTAMP NOT NULL,
    next_due_date TIMESTAMP NOT NULL,
    last_completed_date TIMESTAMP,
    
    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    max_occurrences INTEGER,
    occurrences_count INTEGER DEFAULT 0,
    
    -- Notifications
    notify_user_ids UUID[],
    
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_reminders_tenant_status ON client_reminders(tenant_id, status);
CREATE INDEX idx_reminders_client ON client_reminders(client_id, status);
CREATE INDEX idx_reminders_due_date ON client_reminders(status, next_due_date);

-- Client Reminder History table
CREATE TABLE client_reminder_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reminder_id UUID NOT NULL REFERENCES client_reminders(id) ON DELETE CASCADE,
    completed_by UUID REFERENCES users(id),
    completed_at TIMESTAMP DEFAULT NOW(),
    notes TEXT,
    outcome VARCHAR(20) NOT NULL DEFAULT 'COMPLETED'
);

CREATE INDEX idx_reminder_history_reminder ON client_reminder_history(reminder_id);

-- Notification preferences for reminders
INSERT INTO notification_preferences (user_id, notification_type, enabled)
SELECT id, 'CLIENT_REMINDER_DUE', true FROM users
WHERE NOT EXISTS (
    SELECT 1 FROM notification_preferences np 
    WHERE np.user_id = users.id AND np.notification_type = 'CLIENT_REMINDER_DUE'
);

INSERT INTO notification_preferences (user_id, notification_type, enabled)
SELECT id, 'CLIENT_REMINDER_COMPLETED', true FROM users
WHERE NOT EXISTS (
    SELECT 1 FROM notification_preferences np 
    WHERE np.user_id = users.id AND np.notification_type = 'CLIENT_REMINDER_COMPLETED'
);