-- Fix health_snapshots schema to match entity field names and types
ALTER TABLE health_snapshots
    ALTER COLUMN avg_latency_ms TYPE DOUBLE PRECISION USING avg_latency_ms::DOUBLE PRECISION;

ALTER TABLE health_snapshots
    RENAME COLUMN uptime_pct TO uptime_percent;

ALTER TABLE health_snapshots
    ALTER COLUMN uptime_percent TYPE DOUBLE PRECISION USING uptime_percent::DOUBLE PRECISION;

-- Add missing soft-delete columns (inherited from BaseEntity)
ALTER TABLE licenses
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;

-- licenses.client_id is used for tenant-level trial licenses (no client yet),
-- so remove the NOT NULL constraint and the FK to clients.
ALTER TABLE licenses
    DROP CONSTRAINT IF EXISTS licenses_client_id_fkey;

ALTER TABLE licenses
    ALTER COLUMN client_id DROP NOT NULL;
