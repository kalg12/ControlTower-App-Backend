-- ─────────────────────────────────────────────────────────────────────────────
-- V58: Live Chat Support System
-- ─────────────────────────────────────────────────────────────────────────────

-- Avatar URL on users
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(500);

-- ── chat_conversations ────────────────────────────────────────────────────────
CREATE TABLE chat_conversations (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID         NOT NULL REFERENCES tenants(id),
    visitor_id    VARCHAR(100) NOT NULL,
    visitor_token UUID         NOT NULL UNIQUE,
    visitor_name  VARCHAR(255) NOT NULL,
    visitor_email VARCHAR(255),
    agent_id      UUID         REFERENCES users(id),
    status        VARCHAR(20)  NOT NULL DEFAULT 'WAITING',
    source        VARCHAR(50)  NOT NULL DEFAULT 'POS',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    closed_at     TIMESTAMPTZ,
    archived_at   TIMESTAMPTZ,
    deleted_at    TIMESTAMPTZ
);

CREATE INDEX idx_chat_conv_tenant_status ON chat_conversations (tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_chat_conv_agent         ON chat_conversations (agent_id)          WHERE deleted_at IS NULL;
CREATE INDEX idx_chat_conv_visitor_token ON chat_conversations (visitor_token);

-- ── chat_messages ─────────────────────────────────────────────────────────────
CREATE TABLE chat_messages (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID        NOT NULL REFERENCES chat_conversations(id) ON DELETE CASCADE,
    sender_type     VARCHAR(20) NOT NULL,
    sender_id       UUID,
    content         TEXT        NOT NULL,
    attachment_url  VARCHAR(1000),
    is_read         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_messages_conv   ON chat_messages (conversation_id, created_at);
CREATE INDEX idx_chat_messages_unread ON chat_messages (conversation_id, is_read) WHERE is_read = FALSE;

-- ── chat_transfers ────────────────────────────────────────────────────────────
CREATE TABLE chat_transfers (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID        NOT NULL REFERENCES chat_conversations(id),
    from_agent_id   UUID        NOT NULL REFERENCES users(id),
    to_agent_id     UUID        NOT NULL REFERENCES users(id),
    note            TEXT,
    transferred_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_transfers_conv ON chat_transfers (conversation_id);

-- ── chat_quick_replies ────────────────────────────────────────────────────────
CREATE TABLE chat_quick_replies (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID        NOT NULL REFERENCES tenants(id),
    shortcut   VARCHAR(50) NOT NULL,
    content    TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, shortcut)
);

CREATE INDEX idx_chat_quick_replies_tenant ON chat_quick_replies (tenant_id);

-- ── Seed quick replies ────────────────────────────────────────────────────────
INSERT INTO chat_quick_replies (id, tenant_id, shortcut, content)
SELECT gen_random_uuid(), t.id, '/hola',   '¡Hola! Bienvenido al soporte. ¿En qué le puedo ayudar hoy?'
FROM tenants t WHERE t.deleted_at IS NULL;

INSERT INTO chat_quick_replies (id, tenant_id, shortcut, content)
SELECT gen_random_uuid(), t.id, '/espera', 'Un momento por favor, estoy revisando su caso.'
FROM tenants t WHERE t.deleted_at IS NULL;

INSERT INTO chat_quick_replies (id, tenant_id, shortcut, content)
SELECT gen_random_uuid(), t.id, '/cierre', '¡Gracias por contactarnos! Ha sido un placer ayudarle. ¡Hasta pronto!'
FROM tenants t WHERE t.deleted_at IS NULL;

-- ── Permisos ──────────────────────────────────────────────────────────────────
INSERT INTO permissions (id, code, description, module) VALUES
    (gen_random_uuid(), 'chat:read',   'View chat conversations',                     'chat'),
    (gen_random_uuid(), 'chat:write',  'Send messages and manage conversations',      'chat'),
    (gen_random_uuid(), 'chat:manage', 'Transfer, close and delete conversations',    'chat')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   roles r
CROSS  JOIN permissions p
WHERE  r.code = 'ADMIN'
  AND  r.deleted_at IS NULL
  AND  p.code IN ('chat:read', 'chat:write', 'chat:manage')
ON CONFLICT DO NOTHING;
