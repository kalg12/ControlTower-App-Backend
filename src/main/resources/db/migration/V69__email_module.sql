-- ─────────────────────────────────────────────────────────────────────────────
-- V69: Email Module
-- departments, email_mailbox_configs, email_aliases, email_routing_rules,
-- email_raw, email_ticket_links, email_deliveries
-- + alters on tickets and ticket_comments
-- ─────────────────────────────────────────────────────────────────────────────

-- ── Departments (areas/groups) ────────────────────────────────────────────────
CREATE TABLE departments (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    color       VARCHAR(7),
    icon        VARCHAR(50),
    sla_hours   INTEGER     NOT NULL DEFAULT 24,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, name)
);

CREATE INDEX idx_departments_tenant ON departments (tenant_id, is_active);

-- ── Email Mailbox Configurations (IMAP + SMTP credentials per tenant) ─────────
CREATE TABLE email_mailbox_configs (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name              VARCHAR(100) NOT NULL,
    -- IMAP
    imap_host         VARCHAR(255) NOT NULL,
    imap_port         INTEGER     NOT NULL DEFAULT 993,
    imap_ssl          BOOLEAN     NOT NULL DEFAULT TRUE,
    imap_username     VARCHAR(255) NOT NULL,
    imap_password     TEXT        NOT NULL,  -- AES-256-GCM encrypted
    imap_folder       VARCHAR(100) NOT NULL DEFAULT 'INBOX',
    -- SMTP
    smtp_host         VARCHAR(255) NOT NULL,
    smtp_port         INTEGER     NOT NULL DEFAULT 587,
    smtp_ssl          BOOLEAN     NOT NULL DEFAULT TRUE,
    smtp_username     VARCHAR(255) NOT NULL,
    smtp_password     TEXT        NOT NULL,  -- AES-256-GCM encrypted
    from_email        VARCHAR(255) NOT NULL,
    from_name         VARCHAR(255),
    -- Control
    poll_interval_sec INTEGER     NOT NULL DEFAULT 120,
    last_polled_at    TIMESTAMPTZ,
    last_error        TEXT,
    error_count       INTEGER     NOT NULL DEFAULT 0,
    department_id     UUID        REFERENCES departments(id) ON DELETE SET NULL,
    is_active         BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, imap_username)
);

CREATE INDEX idx_mailbox_configs_tenant_active ON email_mailbox_configs (tenant_id, is_active);
CREATE INDEX idx_mailbox_configs_last_polled   ON email_mailbox_configs (last_polled_at) WHERE is_active = TRUE;

-- ── Email Aliases ─────────────────────────────────────────────────────────────
CREATE TABLE email_aliases (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    mailbox_id    UUID        NOT NULL REFERENCES email_mailbox_configs(id) ON DELETE CASCADE,
    name          VARCHAR(100) NOT NULL,
    alias         VARCHAR(255) NOT NULL,  -- soporte@comerza.com
    department_id UUID        REFERENCES departments(id) ON DELETE SET NULL,
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    forward_to    TEXT[],
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, alias)
);

CREATE INDEX idx_email_aliases_tenant  ON email_aliases (tenant_id, is_active);
CREATE INDEX idx_email_aliases_alias   ON email_aliases (alias);

-- ── Email Routing Rules ───────────────────────────────────────────────────────
CREATE TABLE email_routing_rules (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name        VARCHAR(150) NOT NULL,
    alias_id    UUID        REFERENCES email_aliases(id) ON DELETE SET NULL,
    priority    INTEGER     NOT NULL DEFAULT 100,
    conditions  JSONB       NOT NULL DEFAULT '[]',
    actions     JSONB       NOT NULL DEFAULT '[]',
    match_mode  VARCHAR(10) NOT NULL DEFAULT 'ALL',
    schedule    JSONB,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_routing_rules_tenant   ON email_routing_rules (tenant_id, alias_id, is_active, priority);

-- ── Email Raw (inbound emails) ────────────────────────────────────────────────
CREATE TABLE email_raw (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    mailbox_id      UUID        NOT NULL REFERENCES email_mailbox_configs(id) ON DELETE CASCADE,
    message_id      VARCHAR(500) NOT NULL UNIQUE,  -- Message-ID header
    in_reply_to     VARCHAR(500),
    references_ids  TEXT[],
    from_email      VARCHAR(255) NOT NULL,
    from_name       VARCHAR(255),
    to_email        TEXT[]      NOT NULL,
    cc_email        TEXT[],
    reply_to        VARCHAR(255),
    subject         VARCHAR(1000),
    body_text       TEXT,
    body_html       TEXT,
    headers         JSONB,
    attachments     JSONB       DEFAULT '[]',
    received_at     TIMESTAMPTZ NOT NULL,
    processed_at    TIMESTAMPTZ,
    status          VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    error_message   TEXT,
    spam_score      DECIMAL(5,2),
    is_spam         BOOLEAN     NOT NULL DEFAULT FALSE,
    alias_id        UUID        REFERENCES email_aliases(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_raw_tenant_status   ON email_raw (tenant_id, status, received_at DESC);
CREATE INDEX idx_email_raw_tenant_from     ON email_raw (tenant_id, from_email);
CREATE INDEX idx_email_raw_message_id      ON email_raw (message_id);
CREATE INDEX idx_email_raw_in_reply_to     ON email_raw (in_reply_to) WHERE in_reply_to IS NOT NULL;

-- ── Email ↔ Ticket Links ──────────────────────────────────────────────────────
CREATE TABLE email_ticket_links (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email_id    UUID        NOT NULL REFERENCES email_raw(id) ON DELETE CASCADE,
    ticket_id   UUID        NOT NULL REFERENCES tickets(id)   ON DELETE CASCADE,
    link_type   VARCHAR(30) NOT NULL DEFAULT 'CREATED_FROM',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (email_id, ticket_id)
);

CREATE INDEX idx_email_ticket_links_ticket ON email_ticket_links (ticket_id);
CREATE INDEX idx_email_ticket_links_email  ON email_ticket_links (email_id);

-- ── Email Deliveries (outbound) ───────────────────────────────────────────────
CREATE TABLE email_deliveries (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    mailbox_id       UUID        REFERENCES email_mailbox_configs(id) ON DELETE SET NULL,
    ticket_id        UUID        REFERENCES tickets(id)               ON DELETE SET NULL,
    template_id      UUID,
    from_email       VARCHAR(255) NOT NULL,
    to_email         TEXT[]      NOT NULL,
    cc_email         TEXT[],
    bcc_email        TEXT[],
    reply_to         VARCHAR(255),
    subject          VARCHAR(1000),
    body_html        TEXT,
    in_reply_to      VARCHAR(500),
    status           VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    attempts         INTEGER     NOT NULL DEFAULT 0,
    last_attempt_at  TIMESTAMPTZ,
    next_retry_at    TIMESTAMPTZ,
    error_message    TEXT,
    delivery_type    VARCHAR(30) NOT NULL DEFAULT 'TICKET_REPLY',
    sent_at          TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_deliveries_tenant_status ON email_deliveries (tenant_id, status, created_at DESC);
CREATE INDEX idx_email_deliveries_ticket        ON email_deliveries (ticket_id) WHERE ticket_id IS NOT NULL;
CREATE INDEX idx_email_deliveries_retry         ON email_deliveries (next_retry_at) WHERE status = 'QUEUED' AND attempts > 0;

-- ── Alter tickets: add email and department columns ───────────────────────────
ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS source_email_id UUID REFERENCES email_raw(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS requester_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS department_id   UUID REFERENCES departments(id) ON DELETE SET NULL;

-- ── Alter ticket_comments: add source and email_raw_id ───────────────────────
ALTER TABLE ticket_comments
    ADD COLUMN IF NOT EXISTS source       VARCHAR(20) NOT NULL DEFAULT 'AGENT',
    ADD COLUMN IF NOT EXISTS email_raw_id UUID REFERENCES email_raw(id) ON DELETE SET NULL;

-- ── Permissions for email module ─────────────────────────────────────────────
INSERT INTO permissions (id, code, description, module)
VALUES
    (gen_random_uuid(), 'email:read',   'View email mailboxes, aliases, rules and delivery logs', 'email'),
    (gen_random_uuid(), 'email:write',  'Create and update email mailboxes, aliases and rules',   'email'),
    (gen_random_uuid(), 'email:manage', 'Full email module administration',                        'email')
ON CONFLICT (code) DO NOTHING;
