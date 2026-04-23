-- V57: Payroll / Nómina module (Mexican: IMSS, ISR, INFONAVIT)

CREATE TABLE employees (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    full_name       VARCHAR(200) NOT NULL,
    rfc             VARCHAR(13)  NOT NULL,
    imss_number     VARCHAR(11),
    curp            VARCHAR(18),
    department      VARCHAR(100),
    position        VARCHAR(100),
    salary_type     VARCHAR(20)  NOT NULL CHECK (salary_type IN ('MONTHLY','BIWEEKLY')),
    base_salary     NUMERIC(14,2) NOT NULL CHECK (base_salary >= 0),
    start_date      DATE         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE','TERMINATED')),
    email           VARCHAR(255),
    bank_account    VARCHAR(30),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT uq_employees_rfc_tenant UNIQUE (tenant_id, rfc)
);

CREATE INDEX idx_employees_tenant_status  ON employees (tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_employees_tenant_deleted ON employees (tenant_id, deleted_at);

-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE payroll_periods (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID         NOT NULL,
    year             INT          NOT NULL,
    period_number    INT          NOT NULL,
    period_type      VARCHAR(20)  NOT NULL CHECK (period_type IN ('MENSUAL','QUINCENAL')),
    start_date       DATE         NOT NULL,
    end_date         DATE         NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','PROCESSED','PAID')),
    total_gross      NUMERIC(16,2) NOT NULL DEFAULT 0,
    total_deductions NUMERIC(16,2) NOT NULL DEFAULT 0,
    total_net        NUMERIC(16,2) NOT NULL DEFAULT 0,
    notes            TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ,
    CONSTRAINT uq_payroll_period UNIQUE (tenant_id, year, period_number, period_type)
);

CREATE INDEX idx_payroll_periods_tenant_status ON payroll_periods (tenant_id, status) WHERE deleted_at IS NULL;

-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE payroll_items (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID         NOT NULL,
    period_id        UUID         NOT NULL REFERENCES payroll_periods(id) ON DELETE CASCADE,
    employee_id      UUID         NOT NULL REFERENCES employees(id),
    days_worked      NUMERIC(5,2) NOT NULL DEFAULT 0,
    overtime_hours   NUMERIC(5,2) NOT NULL DEFAULT 0,
    gross_pay        NUMERIC(14,2) NOT NULL DEFAULT 0,
    imss_employee    NUMERIC(14,2) NOT NULL DEFAULT 0,
    isr              NUMERIC(14,2) NOT NULL DEFAULT 0,
    infonavit        NUMERIC(14,2) NOT NULL DEFAULT 0,
    other_deductions NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_deductions NUMERIC(14,2) NOT NULL DEFAULT 0,
    net_pay          NUMERIC(14,2) NOT NULL DEFAULT 0,
    receipt_sent     BOOLEAN      NOT NULL DEFAULT FALSE,
    receipt_sent_at  TIMESTAMPTZ,
    notes            TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_payroll_item UNIQUE (period_id, employee_id)
);

CREATE INDEX idx_payroll_items_period  ON payroll_items (period_id);
CREATE INDEX idx_payroll_items_tenant  ON payroll_items (tenant_id);
CREATE INDEX idx_payroll_items_employee ON payroll_items (employee_id);

-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO permissions (id, name, description) VALUES
    (gen_random_uuid(), 'payroll:read',  'View payroll data'),
    (gen_random_uuid(), 'payroll:write', 'Create and edit payroll data'),
    (gen_random_uuid(), 'payroll:close', 'Close payroll periods and send receipts')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('payroll:read','payroll:write','payroll:close')
ON CONFLICT DO NOTHING;
