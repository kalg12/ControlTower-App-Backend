-- ─────────────────────────────────────────────────────────────────────────────
-- V3: Tenancy — Per-tenant configuration store
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE tenant_configs (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    key         VARCHAR(200) NOT NULL,
    value       TEXT,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, key)
);

CREATE INDEX idx_tenant_configs_tenant ON tenant_configs (tenant_id);
