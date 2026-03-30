-- ─────────────────────────────────────────────────────────────────────────────
-- V5: Audit Log — Immutable event trail
-- No UPDATE or DELETE is ever issued on this table.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE audit_logs (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        REFERENCES tenants(id) ON DELETE SET NULL,
    user_id         UUID        REFERENCES users(id)   ON DELETE SET NULL,
    action          VARCHAR(100) NOT NULL,
    resource_type   VARCHAR(100),
    resource_id     VARCHAR(255),
    old_value       JSONB,
    new_value       JSONB,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    result          VARCHAR(50)  NOT NULL DEFAULT 'SUCCESS',
    correlation_id  VARCHAR(100),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Immutability enforced at application level (no update/delete in repository).
-- Indexes support common query patterns.
CREATE INDEX idx_audit_logs_tenant     ON audit_logs (tenant_id, created_at DESC);
CREATE INDEX idx_audit_logs_user       ON audit_logs (user_id, created_at DESC);
CREATE INDEX idx_audit_logs_action     ON audit_logs (action, created_at DESC);
CREATE INDEX idx_audit_logs_resource   ON audit_logs (resource_type, resource_id);
CREATE INDEX idx_audit_logs_created    ON audit_logs (created_at DESC);
