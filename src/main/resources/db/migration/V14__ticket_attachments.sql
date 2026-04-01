-- Ticket file attachments
CREATE TABLE ticket_attachments (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id    UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    uploaded_by  UUID REFERENCES users(id) ON DELETE SET NULL,
    file_name    VARCHAR(500) NOT NULL,
    content_type VARCHAR(100),
    size_bytes   BIGINT,
    storage_key  VARCHAR(1000) NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ticket_attachments_ticket ON ticket_attachments (ticket_id);

