-- ─────────────────────────────────────────────────────────────────────────────
-- V4: Clients & Branches
-- A Client is an external business managed by a Tenant through Control Tower.
-- A ClientBranch is a physical or logical location of that business.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE clients (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    legal_name  VARCHAR(255),
    tax_id      VARCHAR(100),
    country     VARCHAR(100) NOT NULL DEFAULT 'México',
    status      VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    notes       TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_clients_tenant        ON clients (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_clients_tenant_status ON clients (tenant_id, status) WHERE deleted_at IS NULL;

CREATE TABLE client_branches (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id   UUID        NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    tenant_id   UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    address     VARCHAR(500),
    city        VARCHAR(100),
    country     VARCHAR(100),
    latitude    DECIMAL(10, 7),
    longitude   DECIMAL(10, 7),
    slug        VARCHAR(150),
    status      VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    timezone    VARCHAR(100) NOT NULL DEFAULT 'America/Mexico_City',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_client_branches_client ON client_branches (client_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_client_branches_tenant ON client_branches (tenant_id) WHERE deleted_at IS NULL;

CREATE TABLE client_contacts (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id   UUID        NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    tenant_id   UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    full_name   VARCHAR(255) NOT NULL,
    email       VARCHAR(255),
    phone       VARCHAR(50),
    role        VARCHAR(50)  NOT NULL DEFAULT 'OWNER',
    is_primary  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_client_contacts_client ON client_contacts (client_id);
