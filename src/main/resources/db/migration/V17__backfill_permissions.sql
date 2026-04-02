-- ─────────────────────────────────────────────────────────────────────────────
-- V17: Backfill permissions for tenants onboarded before all permissions
--      were present in the database.
--
-- Idempotent: ON CONFLICT DO NOTHING ensures this is safe to run on any DB.
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. Upsert all system permissions (safe: unique constraint on code)
INSERT INTO permissions (id, code, description, module) VALUES
    (gen_random_uuid(), 'tenant:read',       'View tenant details',        'tenancy'),
    (gen_random_uuid(), 'tenant:write',      'Create/edit tenants',        'tenancy'),
    (gen_random_uuid(), 'user:read',         'View users',                 'identity'),
    (gen_random_uuid(), 'user:write',        'Create/edit users',          'identity'),
    (gen_random_uuid(), 'client:read',       'View clients',               'clients'),
    (gen_random_uuid(), 'client:write',      'Create/edit clients',        'clients'),
    (gen_random_uuid(), 'health:read',       'View health status',         'health'),
    (gen_random_uuid(), 'health:write',      'Manage health rules',        'health'),
    (gen_random_uuid(), 'ticket:read',       'View tickets',               'support'),
    (gen_random_uuid(), 'ticket:write',      'Create/edit tickets',        'support'),
    (gen_random_uuid(), 'license:read',      'View licenses',              'licenses'),
    (gen_random_uuid(), 'license:write',     'Manage licenses',            'licenses'),
    (gen_random_uuid(), 'audit:read',        'View audit logs',            'audit'),
    (gen_random_uuid(), 'notification:read', 'View notifications',         'notifications'),
    (gen_random_uuid(), 'kanban:read',       'View boards and cards',      'kanban'),
    (gen_random_uuid(), 'kanban:write',      'Manage boards and cards',    'kanban'),
    (gen_random_uuid(), 'billing:read',      'View billing information',   'billing'),
    (gen_random_uuid(), 'billing:write',     'Manage billing',             'billing'),
    (gen_random_uuid(), 'integration:read',  'View integrations',          'integrations'),
    (gen_random_uuid(), 'integration:write', 'Manage integrations',        'integrations')
ON CONFLICT (code) DO NOTHING;

-- 2. Grant ALL permissions to every ADMIN role across all tenants
--    (covers roles created by OnboardingService before certain permissions existed)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   roles r
CROSS JOIN permissions p
WHERE  r.code = 'ADMIN'
  AND  r.deleted_at IS NULL
ON CONFLICT DO NOTHING;
