CREATE TABLE calendar_events (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID        NOT NULL REFERENCES tenants(id),
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    event_type   VARCHAR(50)  NOT NULL DEFAULT 'MEETING',
    start_at     TIMESTAMPTZ  NOT NULL,
    end_at       TIMESTAMPTZ  NOT NULL,
    client_id    UUID         REFERENCES clients(id),
    branch_id    UUID         REFERENCES client_branches(id),
    status       VARCHAR(30)  NOT NULL DEFAULT 'SCHEDULED',
    notes        TEXT,
    outcome      TEXT,
    contact_channel VARCHAR(50),
    created_by   UUID         NOT NULL REFERENCES users(id),
    created_at   TIMESTAMPTZ  DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  DEFAULT NOW(),
    deleted_at   TIMESTAMPTZ
);

CREATE TABLE calendar_event_assignees (
    event_id UUID NOT NULL REFERENCES calendar_events(id) ON DELETE CASCADE,
    user_id  UUID NOT NULL REFERENCES users(id),
    PRIMARY KEY (event_id, user_id)
);

CREATE INDEX idx_cal_events_tenant ON calendar_events(tenant_id);
CREATE INDEX idx_cal_events_client ON calendar_events(client_id);
CREATE INDEX idx_cal_events_start  ON calendar_events(start_at);
