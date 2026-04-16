-- ─────────────────────────────────────────────────────────────────────────────
-- V36: Escalation support — escalated_at timestamp on tickets +
--      escalation_rules table for per-priority thresholds
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE tickets ADD COLUMN IF NOT EXISTS escalated_at TIMESTAMPTZ;

CREATE TABLE escalation_rules (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL,
    priority    VARCHAR(20) NOT NULL CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    hours       INTEGER     NOT NULL DEFAULT 24,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, priority)
);

-- Default rules (applied to all existing tenants via a seed)
INSERT INTO escalation_rules (id, tenant_id, priority, hours)
SELECT gen_random_uuid(), t.id,
       u.priority,
       u.hours
FROM   tenants t
CROSS JOIN (
    VALUES ('LOW',48), ('MEDIUM',24), ('HIGH',8), ('CRITICAL',2)
) AS u(priority, hours)
WHERE  t.deleted_at IS NULL
ON CONFLICT (tenant_id, priority) DO NOTHING;
