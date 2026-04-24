-- F27: Email open tracking for proposals
ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS email_tracking_token UUID UNIQUE,
    ADD COLUMN IF NOT EXISTS email_viewed_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_proposals_tracking_token ON proposals (email_tracking_token)
    WHERE email_tracking_token IS NOT NULL;
