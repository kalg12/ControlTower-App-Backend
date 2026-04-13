-- ─────────────────────────────────────────────────────────────────────────────
-- V27: CRM Enhancements — Interactions, Opportunities, Account Owner, Health Score
-- Full CRM functionality with activity logging and sales pipeline.
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. Add CRM fields to clients table
ALTER TABLE clients ADD COLUMN IF NOT EXISTS account_owner_id UUID;
ALTER TABLE clients ADD COLUMN IF NOT EXISTS health_score INTEGER DEFAULT 0;
ALTER TABLE clients ADD COLUMN IF NOT EXISTS total_revenue DOUBLE PRECISION DEFAULT 0;

-- 2. Create client_interactions table (activity log)
CREATE TABLE IF NOT EXISTS client_interactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    client_id       UUID NOT NULL REFERENCES clients(id),
    branch_id       UUID REFERENCES client_branches(id),
    user_id         UUID NOT NULL,
    user_name       VARCHAR(255),
    interaction_type VARCHAR(50) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    occurred_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    ticket_id       UUID,
    outcome         TEXT,
    duration_minutes INTEGER,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_interactions_client_tenant ON client_interactions(client_id, tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_interactions_user_tenant ON client_interactions(user_id, tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_interactions_occurred ON client_interactions(occurred_at DESC) WHERE deleted_at IS NULL;

-- 3. Create client_opportunities table (sales pipeline)
CREATE TABLE IF NOT EXISTS client_opportunities (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    client_id           UUID NOT NULL REFERENCES clients(id),
    branch_id           UUID REFERENCES client_branches(id),
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    value               DOUBLE PRECISION,
    currency            VARCHAR(10) DEFAULT 'MXN',
    stage               VARCHAR(50) NOT NULL DEFAULT 'PROSPECTING',
    probability         INTEGER DEFAULT 10,
    owner_id            UUID,
    owner_name          VARCHAR(255),
    expected_close_date TIMESTAMP,
    closed_date         TIMESTAMP,
    loss_reason         TEXT,
    source              VARCHAR(50),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_opportunities_client_tenant ON client_opportunities(client_id, tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_opportunities_tenant ON client_opportunities(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_opportunities_owner ON client_opportunities(owner_id, tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_opportunities_stage ON client_opportunities(stage) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_opportunities_close_date ON client_opportunities(expected_close_date ASC) WHERE deleted_at IS NULL AND stage NOT IN ('CLOSED_WON', 'CLOSED_LOST');
