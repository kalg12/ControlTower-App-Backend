-- Add primary contact fields to clients table
-- These fields store the main contact information at the company level
-- so it's always visible even if no specific contact person is assigned

ALTER TABLE clients 
ADD COLUMN IF NOT EXISTS primary_phone VARCHAR(50),
ADD COLUMN IF NOT EXISTS primary_email VARCHAR(255),
ADD COLUMN IF NOT EXISTS primary_contact_name VARCHAR(255);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_clients_primary_email ON clients(primary_email) WHERE primary_email IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_clients_primary_phone ON clients(primary_phone) WHERE primary_phone IS NOT NULL;
