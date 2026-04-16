-- ─────────────────────────────────────────────────────────────────────────────
-- V33: Add report:read and report:write permissions and grant to all ADMIN roles.
-- Idempotent: ON CONFLICT DO NOTHING on every INSERT.
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO permissions (id, code, description, module) VALUES
    (gen_random_uuid(), 'report:read',  'View analytics and reports', 'reports'),
    (gen_random_uuid(), 'report:write', 'Manage reports',             'reports')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   roles r
CROSS  JOIN permissions p
WHERE  r.code = 'ADMIN'
  AND  r.deleted_at IS NULL
ON CONFLICT DO NOTHING;
