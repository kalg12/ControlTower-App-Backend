-- ─────────────────────────────────────────────────────────────────────────────
-- V1: Schema baseline
-- Control Tower — Initial migration marker.
-- Domain tables are created in V2+ per module.
-- ─────────────────────────────────────────────────────────────────────────────

-- Enable UUID generation extension (required for gen_random_uuid())
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
