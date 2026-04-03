-- ─────────────────────────────────────────────────────────────────────────────
-- V20: Robust admin permissions backfill
--
-- Ensures every ADMIN role across all tenants has every permission in the
-- system. Uses WHERE NOT EXISTS instead of ON CONFLICT so this is safe even
-- if the role_permissions table somehow lacks the unique/primary key that
-- ON CONFLICT relies on.
--
-- Idempotent: running this multiple times has no side effects.
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. Make sure campaign permissions exist (V18 should have added them,
--    but this is a safety net if V18 ran before the code was deployed)
INSERT INTO permissions (id, code, description, module) VALUES
    (gen_random_uuid(), 'campaign:read',  'View campaigns',   'campaigns'),
    (gen_random_uuid(), 'campaign:write', 'Manage campaigns', 'campaigns')
ON CONFLICT (code) DO NOTHING;

-- 2. Grant EVERY permission to every ADMIN role that is missing any assignment
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   roles r
CROSS JOIN permissions p
WHERE  r.code = 'ADMIN'
  AND  r.deleted_at IS NULL
  AND  NOT EXISTS (
    SELECT 1
    FROM   role_permissions rp
    WHERE  rp.role_id = r.id
      AND  rp.permission_id = p.id
  );
