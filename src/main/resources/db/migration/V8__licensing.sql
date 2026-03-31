-- ─────────────────────────────────────────────────────────────────────────────
-- V8: Licensing — Plans, Features, Licenses, Events
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE plans (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL UNIQUE,
    description     TEXT,
    max_branches    INT         NOT NULL DEFAULT 1,
    max_users       INT         NOT NULL DEFAULT 5,
    price_monthly   NUMERIC(10,2) NOT NULL DEFAULT 0,
    active          BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE plan_features (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id         UUID        NOT NULL REFERENCES plans(id) ON DELETE CASCADE,
    feature_code    VARCHAR(100) NOT NULL,
    enabled         BOOLEAN     NOT NULL DEFAULT TRUE,
    limit_value     INT,
    UNIQUE (plan_id, feature_code)
);

CREATE INDEX idx_plan_features_plan ON plan_features (plan_id);

CREATE TABLE licenses (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    client_id               UUID        NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    plan_id                 UUID        NOT NULL REFERENCES plans(id),
    status                  VARCHAR(30) NOT NULL DEFAULT 'TRIAL',
    current_period_start    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    current_period_end      TIMESTAMP WITH TIME ZONE NOT NULL,
    grace_period_end        TIMESTAMP WITH TIME ZONE,
    stripe_subscription_id  VARCHAR(255),
    notes                   TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (client_id)
);

CREATE INDEX idx_licenses_tenant        ON licenses (tenant_id);
CREATE INDEX idx_licenses_client        ON licenses (client_id);
CREATE INDEX idx_licenses_status        ON licenses (status);
CREATE INDEX idx_licenses_period_end    ON licenses (current_period_end) WHERE status IN ('TRIAL','ACTIVE','GRACE');

CREATE TABLE license_events (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    license_id      UUID        NOT NULL REFERENCES licenses(id) ON DELETE CASCADE,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB,
    processed_at    TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_license_events_license ON license_events (license_id, created_at DESC);

-- ── Seed: default plans ───────────────────────────────────────────────────────
INSERT INTO plans (id, name, description, max_branches, max_users, price_monthly) VALUES
    (gen_random_uuid(), 'Trial',      'Free 14-day trial',                      1,   3,    0.00),
    (gen_random_uuid(), 'Starter',    'Small business plan',                    3,  10,   49.00),
    (gen_random_uuid(), 'Growth',     'Growing business with multiple branches', 10, 50,  149.00),
    (gen_random_uuid(), 'Enterprise', 'Unlimited branches and users',          999, 999, 499.00);
