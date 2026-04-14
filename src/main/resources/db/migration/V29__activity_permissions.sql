-- ─────────────────────────────────────────────────────────────────────────────
-- V29: Activity tracking permissions
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO permissions (id, code, description, module) VALUES
    (gen_random_uuid(), 'activity:read',  'View user activity and navigation logs', 'activity'),
    (gen_random_uuid(), 'activity:write', 'Record page navigation activity',        'activity')
ON CONFLICT (code) DO NOTHING;

-- Grant activity:read to ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   roles r
CROSS JOIN permissions p
WHERE  r.code = 'ADMIN'
  AND  p.code = 'activity:read'
  AND  r.deleted_at IS NULL
ON CONFLICT DO NOTHING;

-- Grant activity:write to all users (everyone's navigation should be tracked)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   roles r
CROSS JOIN permissions p
WHERE  p.code = 'activity:write'
  AND  r.deleted_at IS NULL
ON CONFLICT DO NOTHING;
