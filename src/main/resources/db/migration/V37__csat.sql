-- ─────────────────────────────────────────────────────────────────────────────
-- V37: CSAT (Customer Satisfaction) — survey tokens + responses
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE csat_surveys (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL,
    ticket_id   UUID        NOT NULL,
    token       UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    rating      SMALLINT    CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    sent_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    responded_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_csat_ticket ON csat_surveys (ticket_id);
CREATE INDEX idx_csat_token  ON csat_surveys (token);

INSERT INTO permissions (id, code, description, module) VALUES
    (gen_random_uuid(), 'csat:read',  'View CSAT survey results', 'csat'),
    (gen_random_uuid(), 'csat:write', 'Send and manage CSAT surveys', 'csat')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   roles r
CROSS  JOIN permissions p
WHERE  r.code = 'ADMIN'
  AND  r.deleted_at IS NULL
ON CONFLICT DO NOTHING;
