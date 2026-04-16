-- ─────────────────────────────────────────────────────────────────────────────
-- V34: Knowledge Base — kb_articles table + kb:read/kb:write permissions
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE kb_articles (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL,
    author_id   UUID,
    title       VARCHAR(255) NOT NULL,
    content     TEXT,
    category    VARCHAR(100),
    tags_csv    TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                    CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    views       INTEGER     NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX idx_kb_articles_tenant  ON kb_articles (tenant_id);
CREATE INDEX idx_kb_articles_status  ON kb_articles (tenant_id, status);
CREATE INDEX idx_kb_articles_category ON kb_articles (tenant_id, category);

INSERT INTO permissions (id, code, description, module) VALUES
    (gen_random_uuid(), 'kb:read',  'View knowledge base articles', 'kb'),
    (gen_random_uuid(), 'kb:write', 'Create and edit knowledge base articles', 'kb')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   roles r
CROSS  JOIN permissions p
WHERE  r.code IN ('ADMIN', 'AGENT')
  AND  r.deleted_at IS NULL
ON CONFLICT DO NOTHING;
