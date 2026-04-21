INSERT INTO notification_preferences (user_id, notification_type, enabled)
SELECT id, 'CALENDAR_ASSIGNED', true FROM users
WHERE NOT EXISTS (
    SELECT 1 FROM notification_preferences np 
    WHERE np.user_id = users.id AND np.notification_type = 'CALENDAR_ASSIGNED'
);

INSERT INTO notification_preferences (user_id, notification_type, enabled)
SELECT id, 'CALENDAR_UPDATED', true FROM users
WHERE NOT EXISTS (
    SELECT 1 FROM notification_preferences np 
    WHERE np.user_id = users.id AND np.notification_type = 'CALENDAR_UPDATED'
);

INSERT INTO notification_preferences (user_id, notification_type, enabled)
SELECT id, 'CALENDAR_REMOVED', true FROM users
WHERE NOT EXISTS (
    SELECT 1 FROM notification_preferences np 
    WHERE np.user_id = users.id AND np.notification_type = 'CALENDAR_REMOVED'
);