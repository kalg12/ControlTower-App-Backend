-- ─────────────────────────────────────────────────────────────────────────────
-- V66: New purchase_records table for vendor purchases and POS income imports
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE purchase_records (
    id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID          NOT NULL REFERENCES tenants(id),
    vendor               VARCHAR(200),
    description          VARCHAR(500)  NOT NULL,
    amount               NUMERIC(12,2) NOT NULL,
    currency             VARCHAR(10)   NOT NULL DEFAULT 'MXN',
    category             VARCHAR(50)   NOT NULL DEFAULT 'OTHER'
                         CHECK (category IN ('PAYROLL','SERVICES','RENT','MARKETING','TECH','TRAVEL','SUPPLIES','TAXES','OTHER')),
    quantity             NUMERIC(10,2) NOT NULL DEFAULT 1,
    unit_price           NUMERIC(12,2),
    receipt_url          VARCHAR(1000),
    notes                TEXT,
    purchased_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    source               VARCHAR(20)   NOT NULL DEFAULT 'MANUAL'
                         CHECK (source IN ('MANUAL','POS_IMPORT')),
    pos_reference        VARCHAR(200),
    is_recurring         BOOLEAN       NOT NULL DEFAULT false,
    recurrence_type      VARCHAR(20)
                         CHECK (recurrence_type IN ('DAILY','WEEKLY','BIWEEKLY','MONTHLY','QUARTERLY','YEARLY')),
    recurrence_end_date  DATE,
    next_occurrence_date DATE,
    parent_recurring_id  UUID          REFERENCES purchase_records(id) ON DELETE SET NULL,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    deleted_at           TIMESTAMPTZ
);

CREATE INDEX idx_purchase_records_tenant      ON purchase_records (tenant_id);
CREATE INDEX idx_purchase_records_source      ON purchase_records (tenant_id, source);
CREATE INDEX idx_purchase_records_purchased   ON purchase_records (tenant_id, purchased_at DESC);
CREATE INDEX idx_purchase_records_parent      ON purchase_records (parent_recurring_id);
CREATE INDEX idx_purchase_records_recurring   ON purchase_records (next_occurrence_date) WHERE is_recurring = true AND deleted_at IS NULL;
