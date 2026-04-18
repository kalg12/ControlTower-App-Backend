CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_type VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    UNIQUE (user_id, notification_type)
);

CREATE INDEX idx_notif_prefs_user ON notification_preferences(user_id);
