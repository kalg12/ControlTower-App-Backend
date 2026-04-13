-- ─────────────────────────────────────────────────────────────────────────────
-- V26: Client CRM fields + contact notes
-- Adds sales/marketing metadata to clients and a notes field to contacts.
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS website      VARCHAR(500),
    ADD COLUMN IF NOT EXISTS industry     VARCHAR(100),
    ADD COLUMN IF NOT EXISTS segment      VARCHAR(50);

ALTER TABLE client_contacts
    ADD COLUMN IF NOT EXISTS notes        TEXT;
