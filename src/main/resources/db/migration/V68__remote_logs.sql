CREATE TABLE remote_logs (
    id            UUID         NOT NULL PRIMARY KEY,
    tenant_id     UUID         NOT NULL,
    endpoint_id   UUID         REFERENCES integration_endpoints(id) ON DELETE SET NULL,
    level         VARCHAR(20)  NOT NULL,
    service_name  VARCHAR(255),
    message       TEXT         NOT NULL,
    stack_trace   TEXT,
    business_name VARCHAR(255),
    source        VARCHAR(100) NOT NULL DEFAULT 'POS',
    metadata      JSONB,
    received_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_remote_logs_tenant_received ON remote_logs(tenant_id, received_at DESC);
CREATE INDEX idx_remote_logs_level           ON remote_logs(level);
CREATE INDEX idx_remote_logs_service         ON remote_logs(service_name);
