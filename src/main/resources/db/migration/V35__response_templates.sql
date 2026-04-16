-- ─────────────────────────────────────────────────────────────────────────────
-- V35: Response templates (macros) + template:read/write permissions
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE response_templates (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL,
    author_id   UUID,
    name        VARCHAR(120) NOT NULL,
    body        TEXT        NOT NULL,
    category    VARCHAR(80),
    shortcut    VARCHAR(40),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX idx_rtpl_tenant ON response_templates (tenant_id);

INSERT INTO permissions (id, code, description, module) VALUES
    (gen_random_uuid(), 'template:read',  'View response templates', 'templates'),
    (gen_random_uuid(), 'template:write', 'Create and edit response templates', 'templates')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   roles r
CROSS  JOIN permissions p
WHERE  r.code IN ('ADMIN', 'AGENT')
  AND  r.deleted_at IS NULL
ON CONFLICT DO NOTHING;
