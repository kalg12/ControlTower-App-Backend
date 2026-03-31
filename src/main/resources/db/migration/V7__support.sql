-- ─────────────────────────────────────────────────────────────────────────────
-- V7: Support — Tickets, Comments, SLA, History
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE tickets (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id)         ON DELETE CASCADE,
    client_id       UUID        REFERENCES clients(id)                  ON DELETE SET NULL,
    branch_id       UUID        REFERENCES client_branches(id)          ON DELETE SET NULL,
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    priority        VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    status          VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    assignee_id     UUID        REFERENCES users(id)                    ON DELETE SET NULL,
    source          VARCHAR(30)  NOT NULL DEFAULT 'MANUAL',
    source_ref_id   VARCHAR(255),
    labels          TEXT[],
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_tickets_tenant        ON tickets (tenant_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_tickets_status        ON tickets (tenant_id, status)          WHERE deleted_at IS NULL;
CREATE INDEX idx_tickets_assignee      ON tickets (assignee_id)                WHERE deleted_at IS NULL;
CREATE INDEX idx_tickets_client        ON tickets (client_id)                  WHERE deleted_at IS NULL;

CREATE TABLE ticket_comments (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id   UUID        NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    author_id   UUID        REFERENCES users(id)            ON DELETE SET NULL,
    content     TEXT        NOT NULL,
    is_internal BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ticket_comments_ticket ON ticket_comments (ticket_id, created_at);

CREATE TABLE ticket_slas (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id   UUID        NOT NULL UNIQUE REFERENCES tickets(id) ON DELETE CASCADE,
    due_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    breached_at TIMESTAMP WITH TIME ZONE,
    breached    BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_ticket_slas_due    ON ticket_slas (due_at)     WHERE NOT breached;
CREATE INDEX idx_ticket_slas_breach ON ticket_slas (breached)   WHERE NOT breached;

CREATE TABLE ticket_history (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id   UUID        NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    changed_by  UUID        REFERENCES users(id)            ON DELETE SET NULL,
    field       VARCHAR(100) NOT NULL,
    old_value   VARCHAR(500),
    new_value   VARCHAR(500),
    changed_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ticket_history_ticket ON ticket_history (ticket_id, changed_at DESC);
