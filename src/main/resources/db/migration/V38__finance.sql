-- ─────────────────────────────────────────────────────────────────────────────
-- V38: Finance module — invoices, line items, payments, expenses
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE invoices (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL REFERENCES tenants(id),
    client_id   UUID        REFERENCES clients(id),
    number      VARCHAR(50) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                CHECK (status IN ('DRAFT','SENT','PAID','OVERDUE','CANCELLED','VOIDED')),
    subtotal    NUMERIC(12,2) NOT NULL DEFAULT 0,
    tax_rate    NUMERIC(5,2)  NOT NULL DEFAULT 16.00,
    tax_amount  NUMERIC(12,2) NOT NULL DEFAULT 0,
    total       NUMERIC(12,2) NOT NULL DEFAULT 0,
    currency    VARCHAR(10)   NOT NULL DEFAULT 'MXN',
    notes       TEXT,
    issued_at   DATE,
    due_date    DATE,
    paid_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    UNIQUE (tenant_id, number)
);

CREATE TABLE invoice_line_items (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id  UUID         NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description VARCHAR(500) NOT NULL,
    quantity    NUMERIC(10,2) NOT NULL DEFAULT 1,
    unit_price  NUMERIC(12,2) NOT NULL,
    total       NUMERIC(12,2) NOT NULL,
    position    INT           NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE payments (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL REFERENCES tenants(id),
    client_id   UUID         REFERENCES clients(id),
    invoice_id  UUID         REFERENCES invoices(id),
    amount      NUMERIC(12,2) NOT NULL,
    currency    VARCHAR(10)   NOT NULL DEFAULT 'MXN',
    method      VARCHAR(30)   NOT NULL DEFAULT 'BANK_TRANSFER'
                CHECK (method IN ('BANK_TRANSFER','CASH','CARD','CHECK','CRYPTO','OTHER')),
    reference   VARCHAR(200),
    notes       TEXT,
    paid_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);

CREATE TABLE expenses (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL REFERENCES tenants(id),
    category    VARCHAR(50)  NOT NULL DEFAULT 'OTHER'
                CHECK (category IN ('PAYROLL','SERVICES','RENT','MARKETING','TECH','TRAVEL','SUPPLIES','TAXES','OTHER')),
    description VARCHAR(500) NOT NULL,
    amount      NUMERIC(12,2) NOT NULL,
    currency    VARCHAR(10)   NOT NULL DEFAULT 'MXN',
    vendor      VARCHAR(200),
    receipt_url VARCHAR(1000),
    notes       TEXT,
    paid_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX idx_invoices_tenant ON invoices (tenant_id);
CREATE INDEX idx_invoices_client ON invoices (client_id);
CREATE INDEX idx_invoices_status ON invoices (tenant_id, status);
CREATE INDEX idx_payments_tenant ON payments (tenant_id);
CREATE INDEX idx_expenses_tenant ON expenses (tenant_id);

INSERT INTO permissions (id, code, description, module) VALUES
    (gen_random_uuid(), 'finance:read',  'View invoices, payments and expenses', 'finance'),
    (gen_random_uuid(), 'finance:write', 'Create and manage financial records',  'finance')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   roles r
CROSS  JOIN permissions p
WHERE  r.code = 'ADMIN'
  AND  r.deleted_at IS NULL
ON CONFLICT DO NOTHING;
