-- Persons: individual CRM contacts (B2C or standalone, not necessarily tied to a company)
CREATE TABLE persons (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id),
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100),
    email       VARCHAR(255),
    phone       VARCHAR(50),
    whatsapp    VARCHAR(50),
    birth_date  DATE,
    notes       TEXT,
    lead_source VARCHAR(50),
    status      VARCHAR(30)  NOT NULL DEFAULT 'PROSPECT',
    assigned_to_id UUID REFERENCES users(id),
    client_id   UUID REFERENCES clients(id),
    address     VARCHAR(500),
    tags        TEXT[],
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX idx_persons_tenant  ON persons(tenant_id);
CREATE INDEX idx_persons_email   ON persons(email);
CREATE INDEX idx_persons_client  ON persons(client_id);
CREATE INDEX idx_persons_status  ON persons(tenant_id, status) WHERE deleted_at IS NULL;

-- Link calendar events to persons (in addition to existing client link)
ALTER TABLE calendar_events ADD COLUMN person_id UUID REFERENCES persons(id);
CREATE INDEX idx_cal_events_person ON calendar_events(person_id);
