-- Backfill chat:read to all non-ADMIN roles that already have any :read permission.
-- V58 assigned chat permissions only to ADMIN; preset roles (MEMBER, CONTRIBUTOR, AGENT, etc.)
-- were created in V23 before chat:read existed and so never received it.

INSERT INTO role_permissions (role_id, permission_id)
SELECT DISTINCT r.id, p.id
FROM   roles r
CROSS  JOIN permissions p
WHERE  r.deleted_at IS NULL
  AND  r.code <> 'ADMIN'
  AND  p.code = 'chat:read'
  AND  EXISTS (
       SELECT 1 FROM role_permissions rp2
       JOIN permissions p2 ON p2.id = rp2.permission_id
       WHERE rp2.role_id = r.id AND p2.code LIKE '%:read'
  )
ON CONFLICT DO NOTHING;

-- Backfill chat:write to non-ADMIN roles that already have other :write permissions.
INSERT INTO role_permissions (role_id, permission_id)
SELECT DISTINCT r.id, p.id
FROM   roles r
CROSS  JOIN permissions p
WHERE  r.deleted_at IS NULL
  AND  r.code <> 'ADMIN'
  AND  p.code = 'chat:write'
  AND  EXISTS (
       SELECT 1 FROM role_permissions rp2
       JOIN permissions p2 ON p2.id = rp2.permission_id
       WHERE rp2.role_id = r.id AND p2.code LIKE '%:write'
  )
ON CONFLICT DO NOTHING;
