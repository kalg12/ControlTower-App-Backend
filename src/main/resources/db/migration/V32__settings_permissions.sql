-- ─────────────────────────────────────────────────────────────────────────────
-- V32: Add settings:read and settings:write permissions and grant them to all
--      ADMIN roles. Also backfill ticket:read / ticket:write on ADMIN roles
--      (covers the time-tracking endpoints that require those codes).
-- Idempotent: ON CONFLICT DO NOTHING on every INSERT.
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. Upsert new permissions
INSERT INTO permissions (id, code, description, module) VALUES
    (gen_random_uuid(), 'settings:read',  'View application settings',  'settings'),
    (gen_random_uuid(), 'settings:write', 'Modify application settings', 'settings')
ON CONFLICT (code) DO NOTHING;

-- 2. Grant ALL permissions (including the new ones) to every ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   roles r
CROSS  JOIN permissions p
WHERE  r.code = 'ADMIN'
  AND  r.deleted_at IS NULL
ON CONFLICT DO NOTHING;
