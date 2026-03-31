-- ─────────────────────────────────────────────────────────────────────────────
-- V12: Billing — Stripe Customers, Webhook Events, Billing Events
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE stripe_customers (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    client_id               UUID        NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    stripe_customer_id      VARCHAR(255) NOT NULL UNIQUE,
    stripe_subscription_id  VARCHAR(255),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (client_id)
);

CREATE INDEX idx_stripe_customers_tenant     ON stripe_customers (tenant_id);
CREATE INDEX idx_stripe_customers_stripe_id  ON stripe_customers (stripe_customer_id);

CREATE TABLE stripe_webhook_events (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    stripe_event_id VARCHAR(255) NOT NULL UNIQUE,
    type            VARCHAR(100) NOT NULL,
    payload         JSONB        NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'RECEIVED',
    error_message   TEXT,
    received_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_stripe_webhook_events_status ON stripe_webhook_events (status) WHERE status = 'RECEIVED';
CREATE INDEX idx_stripe_webhook_events_type   ON stripe_webhook_events (type);

CREATE TABLE billing_events (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    client_id       UUID        REFERENCES clients(id) ON DELETE SET NULL,
    event_type      VARCHAR(100) NOT NULL,
    amount          NUMERIC(10,2),
    currency        VARCHAR(10),
    stripe_event_id VARCHAR(255),
    metadata        JSONB,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_billing_events_tenant ON billing_events (tenant_id, created_at DESC);
CREATE INDEX idx_billing_events_client ON billing_events (client_id);
