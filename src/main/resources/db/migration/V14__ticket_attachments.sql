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

-- Health daily snapshots
CREATE TABLE health_snapshots (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID NOT NULL,
    branch_id        UUID NOT NULL,
    snapshot_date    DATE NOT NULL,
    uptime_percent   DOUBLE PRECISION,
    avg_latency_ms   DOUBLE PRECISION,
    check_count      INT NOT NULL DEFAULT 0,
    incident_count   INT NOT NULL DEFAULT 0,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_snapshot_branch_date UNIQUE (branch_id, snapshot_date)
);
CREATE INDEX idx_health_snapshots_tenant ON health_snapshots (tenant_id);
CREATE INDEX idx_health_snapshots_branch ON health_snapshots (branch_id);
