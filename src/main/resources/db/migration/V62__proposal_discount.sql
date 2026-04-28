-- V62: Add discount fields to proposals
ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS discount_type   VARCHAR(20),
    ADD COLUMN IF NOT EXISTS discount_value  NUMERIC(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0;
