-- ─────────────────────────────────────────────────────────────────────────────
-- V18: Campaigns module
-- Stores email/SMS/push/in-app marketing campaigns per tenant.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE campaigns (
    id               UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id        UUID        NOT NULL,
    name             VARCHAR(255) NOT NULL,
    type             VARCHAR(20)  NOT NULL,   -- EMAIL | SMS | PUSH | IN_APP
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT', -- DRAFT | SCHEDULED | SENT | FAILED | CANCELED
    subject          VARCHAR(500),
    body             TEXT         NOT NULL,
    target_audience  VARCHAR(255),
    sent_count       INTEGER      NOT NULL DEFAULT 0,
    open_rate        DOUBLE PRECISION,
    scheduled_at     TIMESTAMPTZ,
    sent_at          TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ
);

CREATE INDEX idx_campaigns_tenant ON campaigns (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_campaigns_status  ON campaigns (tenant_id, status) WHERE deleted_at IS NULL;

-- Add campaign:read / campaign:write permissions and grant to ADMIN roles
INSERT INTO permissions (id, code, description, module) VALUES
    (gen_random_uuid(), 'campaign:read',  'View campaigns',   'campaigns'),
    (gen_random_uuid(), 'campaign:write', 'Manage campaigns', 'campaigns')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   roles r
CROSS JOIN permissions p
WHERE  r.code = 'ADMIN'
  AND  r.deleted_at IS NULL
  AND  p.code IN ('campaign:read', 'campaign:write')
ON CONFLICT DO NOTHING;
