-- Remove DKIM columns — feature removed
ALTER TABLE email_mailbox_configs
    DROP COLUMN IF EXISTS dkim_selector,
    DROP COLUMN IF EXISTS dkim_private_key;
