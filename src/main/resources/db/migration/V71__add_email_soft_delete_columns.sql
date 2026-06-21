-- Email aggregate entities that extend BaseEntity inherit its soft-delete field.
-- Keep the database schema aligned with that shared JPA mapping.
ALTER TABLE departments
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE email_mailbox_configs
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE email_aliases
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE email_routing_rules
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
