-- chat_ratings: one rating per closed/archived conversation submitted by the visitor
CREATE TABLE IF NOT EXISTS chat_ratings (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID         NOT NULL REFERENCES chat_conversations(id) ON DELETE CASCADE,
    tenant_id       UUID         NOT NULL REFERENCES tenants(id),
    rating          SMALLINT     NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment         VARCHAR(1000),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_chat_rating_per_conv UNIQUE (conversation_id)
);

-- SUPPORT_AGENT preset role: full chat permissions, one per existing tenant
INSERT INTO roles (id, tenant_id, name, code, description, is_system, created_at, updated_at)
SELECT gen_random_uuid(), t.id, 'Agente de Soporte', 'SUPPORT_AGENT',
       'Attend and manage live-chat conversations', false, NOW(), NOW()
FROM tenants t
WHERE t.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles r
      WHERE r.tenant_id = t.id AND r.code = 'SUPPORT_AGENT' AND r.deleted_at IS NULL
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('chat:read', 'chat:write', 'chat:manage')
WHERE r.code = 'SUPPORT_AGENT' AND r.deleted_at IS NULL
ON CONFLICT DO NOTHING;
