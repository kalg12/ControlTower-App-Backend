-- ─────────────────────────────────────────────────────────────────────────────
-- V11: Generic Integrations — Endpoints, Events, Webhook Deliveries
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE integration_endpoints (
    id                          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                   UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    client_branch_id            UUID        REFERENCES client_branches(id) ON DELETE SET NULL,
    type                        VARCHAR(30)  NOT NULL DEFAULT 'CUSTOM',
    pull_url                    VARCHAR(1000),
    api_key                     VARCHAR(500),
    heartbeat_interval_seconds  INT         NOT NULL DEFAULT 300,
    contract_version            VARCHAR(50),
    metadata                    JSONB,
    active                      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at                  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_integration_endpoints_tenant ON integration_endpoints (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_integration_endpoints_branch ON integration_endpoints (client_branch_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_integration_endpoints_active ON integration_endpoints (active) WHERE deleted_at IS NULL AND active = TRUE;

CREATE TABLE integration_events (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    endpoint_id     UUID        NOT NULL REFERENCES integration_endpoints(id) ON DELETE CASCADE,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB,
    received_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_integration_events_endpoint ON integration_events (endpoint_id, received_at DESC);

CREATE TABLE webhook_deliveries (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    endpoint_id     UUID        NOT NULL REFERENCES integration_endpoints(id) ON DELETE CASCADE,
    url             VARCHAR(1000) NOT NULL,
    payload         JSONB,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts        INT         NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    response_status INT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_webhook_deliveries_endpoint ON webhook_deliveries (endpoint_id, created_at DESC);
CREATE INDEX idx_webhook_deliveries_pending  ON webhook_deliveries (status, attempts) WHERE status = 'PENDING';
