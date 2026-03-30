-- ─────────────────────────────────────────────────────────────────────────────
-- V6: Health Monitoring
-- ─────────────────────────────────────────────────────────────────────────────

-- Individual health check results (one record per check/heartbeat)
CREATE TABLE health_checks (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id)         ON DELETE CASCADE,
    branch_id       UUID        NOT NULL REFERENCES client_branches(id)  ON DELETE CASCADE,
    checked_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    status          VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN',
    latency_ms      INTEGER,
    error_message   TEXT,
    version         VARCHAR(100),
    source          VARCHAR(30)  NOT NULL DEFAULT 'HEARTBEAT',
    metadata        JSONB
);

CREATE INDEX idx_health_checks_branch  ON health_checks (branch_id, checked_at DESC);
CREATE INDEX idx_health_checks_tenant  ON health_checks (tenant_id, checked_at DESC);
CREATE INDEX idx_health_checks_status  ON health_checks (branch_id, status, checked_at DESC);

-- Active incidents (auto-opened when health degrades, auto-closed on recovery)
CREATE TABLE health_incidents (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id)         ON DELETE CASCADE,
    branch_id       UUID        NOT NULL REFERENCES client_branches(id)  ON DELETE CASCADE,
    opened_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    resolved_at     TIMESTAMP WITH TIME ZONE,
    severity        VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    description     TEXT,
    auto_created    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_health_incidents_branch ON health_incidents (branch_id, opened_at DESC);
CREATE INDEX idx_health_incidents_open   ON health_incidents (tenant_id, resolved_at)
    WHERE resolved_at IS NULL;

-- Rules that define when to open incidents and how to alert
CREATE TABLE health_rules (
    id                  UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID    NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id           UUID    REFERENCES client_branches(id)  ON DELETE CASCADE,
    rule_type           VARCHAR(50)  NOT NULL,
    threshold_value     INTEGER,
    severity            VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    alert_channel       VARCHAR(50),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_health_rules_branch ON health_rules (branch_id) WHERE is_active;
CREATE INDEX idx_health_rules_tenant ON health_rules (tenant_id) WHERE is_active;

-- Daily health snapshots for historical trend analysis
CREATE TABLE health_snapshots (
    id              UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID    NOT NULL REFERENCES tenants(id)         ON DELETE CASCADE,
    branch_id       UUID    NOT NULL REFERENCES client_branches(id)  ON DELETE CASCADE,
    snapshot_date   DATE    NOT NULL,
    uptime_pct      DECIMAL(5,2),
    avg_latency_ms  INTEGER,
    check_count     INTEGER,
    incident_count  INTEGER,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (branch_id, snapshot_date)
);

CREATE INDEX idx_health_snapshots_branch ON health_snapshots (branch_id, snapshot_date DESC);
