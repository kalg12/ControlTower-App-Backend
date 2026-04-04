-- Read-only "Member" role per tenant (for inviting users without full admin).
INSERT INTO roles (id, tenant_id, name, code, description, system, created_at, updated_at)
SELECT gen_random_uuid(), t.id, 'Member', 'MEMBER', 'Read-only access to modules (view dashboards, boards, tickets, etc.)', false, NOW(), NOW()
FROM tenants t
WHERE t.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles r
      WHERE r.tenant_id = t.id AND r.code = 'MEMBER' AND r.deleted_at IS NULL
  );

-- Grant every *:read permission to MEMBER roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code LIKE '%:read'
WHERE r.code = 'MEMBER'
  AND r.deleted_at IS NULL
ON CONFLICT DO NOTHING;
