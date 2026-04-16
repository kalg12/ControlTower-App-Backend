-- ─────────────────────────────────────────────────────────────────────────────
-- V39: Tenant locale (country, timezone, currency) + Client CRM fields
-- ─────────────────────────────────────────────────────────────────────────────

-- Tenant locale fields
ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS country  VARCHAR(100) NOT NULL DEFAULT 'México',
    ADD COLUMN IF NOT EXISTS timezone VARCHAR(100) NOT NULL DEFAULT 'America/Mexico_City',
    ADD COLUMN IF NOT EXISTS currency VARCHAR(10)  NOT NULL DEFAULT 'MXN';

-- Client CRM enrichment
ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS lead_source VARCHAR(50),
    ADD COLUMN IF NOT EXISTS phone       VARCHAR(50);
