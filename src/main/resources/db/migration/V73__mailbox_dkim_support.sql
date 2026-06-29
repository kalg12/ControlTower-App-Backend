-- ── DKIM signing support per mailbox ─────────────────────────────────────────
-- dkim_selector:    DNS selector, e.g. "mail" → mail._domainkey.domain.com
-- dkim_private_key: AES-256-GCM encrypted PKCS#8 PEM private key
ALTER TABLE email_mailbox_configs
    ADD COLUMN IF NOT EXISTS dkim_selector     VARCHAR(64),
    ADD COLUMN IF NOT EXISTS dkim_private_key  TEXT;
