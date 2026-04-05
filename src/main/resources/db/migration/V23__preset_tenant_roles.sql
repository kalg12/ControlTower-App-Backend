-- Extra preset roles per tenant (Contributor, Billing, Marketing) for one-click assignment when inviting users.
-- Uses is_system to match JPA @Column(name = "is_system").
--
-- Also (re)ensure MEMBER exists: V22 used column name "system" which does not match is_system — tenants may lack MEMBER.

INSERT INTO roles (id, tenant_id, name, code, description, is_system, created_at, updated_at)
SELECT gen_random_uuid(), t.id, 'Member', 'MEMBER',
  'Read-only access to modules (view dashboards, boards, tickets, etc.)', false, NOW(), NOW()
FROM tenants t
WHERE t.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles r
      WHERE r.tenant_id = t.id AND r.code = 'MEMBER' AND r.deleted_at IS NULL
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code LIKE '%:read'
WHERE r.code = 'MEMBER' AND r.deleted_at IS NULL
ON CONFLICT DO NOTHING;

-- Contributor: all *:read + ticket/kanban/client write
INSERT INTO roles (id, tenant_id, name, code, description, is_system, created_at, updated_at)
SELECT gen_random_uuid(), t.id, 'Contributor', 'CONTRIBUTOR',
  'View all modules; edit tickets, Kanban boards, and clients', false, NOW(), NOW()
FROM tenants t
WHERE t.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles r
      WHERE r.tenant_id = t.id AND r.code = 'CONTRIBUTOR' AND r.deleted_at IS NULL
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON (
    p.code LIKE '%:read'
    OR p.code IN ('ticket:write', 'kanban:write', 'client:write')
)
WHERE r.code = 'CONTRIBUTOR' AND r.deleted_at IS NULL
ON CONFLICT DO NOTHING;

-- Billing: all *:read + billing:write
INSERT INTO roles (id, tenant_id, name, code, description, is_system, created_at, updated_at)
SELECT gen_random_uuid(), t.id, 'Billing', 'BILLING',
  'View all modules; manage billing', false, NOW(), NOW()
FROM tenants t
WHERE t.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles r
      WHERE r.tenant_id = t.id AND r.code = 'BILLING' AND r.deleted_at IS NULL
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON (p.code LIKE '%:read' OR p.code = 'billing:write')
WHERE r.code = 'BILLING' AND r.deleted_at IS NULL
ON CONFLICT DO NOTHING;

-- Marketing: all *:read + campaign:write
INSERT INTO roles (id, tenant_id, name, code, description, is_system, created_at, updated_at)
SELECT gen_random_uuid(), t.id, 'Marketing', 'MARKETING',
  'View all modules; manage campaigns', false, NOW(), NOW()
FROM tenants t
WHERE t.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles r
      WHERE r.tenant_id = t.id AND r.code = 'MARKETING' AND r.deleted_at IS NULL
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON (p.code LIKE '%:read' OR p.code = 'campaign:write')
WHERE r.code = 'MARKETING' AND r.deleted_at IS NULL
ON CONFLICT DO NOTHING;
