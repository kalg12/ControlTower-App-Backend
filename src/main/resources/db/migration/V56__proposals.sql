-- V56: Proposals module — proposals + proposal_line_items + permissions + expense indexes

CREATE TABLE proposals (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID          NOT NULL REFERENCES tenants(id),
    client_id       UUID          NOT NULL REFERENCES clients(id),
    number          VARCHAR(50)   NOT NULL,
    title           VARCHAR(255)  NOT NULL,
    description     TEXT,
    status          VARCHAR(20)   NOT NULL DEFAULT 'DRAFT'
                    CHECK (status IN ('DRAFT','SENT','VIEWED','ACCEPTED','REJECTED','EXPIRED')),
    subtotal        NUMERIC(12,2) NOT NULL DEFAULT 0,
    tax_rate        NUMERIC(5,2)  NOT NULL DEFAULT 16.00,
    tax_amount      NUMERIC(12,2) NOT NULL DEFAULT 0,
    total           NUMERIC(12,2) NOT NULL DEFAULT 0,
    currency        VARCHAR(10)   NOT NULL DEFAULT 'MXN',
    validity_date   DATE,
    notes           TEXT,
    terms           TEXT,
    sent_at         TIMESTAMPTZ,
    viewed_at       TIMESTAMPTZ,
    accepted_at     TIMESTAMPTZ,
    rejected_at     TIMESTAMPTZ,
    sent_by_id      UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    UNIQUE (tenant_id, number)
);

CREATE TABLE proposal_line_items (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    proposal_id  UUID          NOT NULL REFERENCES proposals(id) ON DELETE CASCADE,
    description  VARCHAR(500)  NOT NULL,
    quantity     NUMERIC(10,2) NOT NULL DEFAULT 1,
    unit_price   NUMERIC(12,2) NOT NULL,
    subtotal     NUMERIC(12,2) NOT NULL,
    position     INT           NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_proposals_tenant       ON proposals (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_proposals_client       ON proposals (client_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_proposals_status       ON proposals (tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_proposals_validity     ON proposals (validity_date) WHERE deleted_at IS NULL AND status IN ('SENT','VIEWED');
CREATE INDEX idx_proposal_items_parent  ON proposal_line_items (proposal_id);

CREATE INDEX IF NOT EXISTS idx_expenses_paid_at       ON expenses (tenant_id, paid_at) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_expenses_category_date ON expenses (tenant_id, category, paid_at) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_expenses_vendor        ON expenses (tenant_id, vendor) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_expenses_amount        ON expenses (tenant_id, amount) WHERE deleted_at IS NULL;

INSERT INTO permissions (id, code, description, module) VALUES
    (gen_random_uuid(), 'proposal:read',  'View proposals',                  'proposals'),
    (gen_random_uuid(), 'proposal:write', 'Create, edit and send proposals', 'proposals')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   roles r
CROSS  JOIN permissions p
WHERE  r.code = 'ADMIN'
  AND  r.deleted_at IS NULL
  AND  p.code IN ('proposal:read', 'proposal:write')
ON CONFLICT DO NOTHING;
