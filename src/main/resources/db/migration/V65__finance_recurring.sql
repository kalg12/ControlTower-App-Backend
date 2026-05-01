-- ─────────────────────────────────────────────────────────────────────────────
-- V65: Add recurring-transaction columns to invoices, payments, and expenses
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE invoices
    ADD COLUMN is_recurring           BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN recurrence_type        VARCHAR(20)
                                      CHECK (recurrence_type IN ('DAILY','WEEKLY','BIWEEKLY','MONTHLY','QUARTERLY','YEARLY')),
    ADD COLUMN recurrence_end_date    DATE,
    ADD COLUMN next_occurrence_date   DATE,
    ADD COLUMN parent_recurring_id    UUID REFERENCES invoices(id) ON DELETE SET NULL;

ALTER TABLE payments
    ADD COLUMN source                 VARCHAR(20) NOT NULL DEFAULT 'MANUAL'
                                      CHECK (source IN ('MANUAL','POS_IMPORT')),
    ADD COLUMN pos_reference          VARCHAR(200),
    ADD COLUMN is_recurring           BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN recurrence_type        VARCHAR(20)
                                      CHECK (recurrence_type IN ('DAILY','WEEKLY','BIWEEKLY','MONTHLY','QUARTERLY','YEARLY')),
    ADD COLUMN recurrence_end_date    DATE,
    ADD COLUMN next_occurrence_date   DATE,
    ADD COLUMN parent_recurring_id    UUID REFERENCES payments(id) ON DELETE SET NULL;

ALTER TABLE expenses
    ADD COLUMN is_recurring           BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN recurrence_type        VARCHAR(20)
                                      CHECK (recurrence_type IN ('DAILY','WEEKLY','BIWEEKLY','MONTHLY','QUARTERLY','YEARLY')),
    ADD COLUMN recurrence_end_date    DATE,
    ADD COLUMN next_occurrence_date   DATE,
    ADD COLUMN parent_recurring_id    UUID REFERENCES expenses(id) ON DELETE SET NULL;

CREATE INDEX idx_invoices_recurring ON invoices (next_occurrence_date) WHERE is_recurring = true AND deleted_at IS NULL;
CREATE INDEX idx_payments_recurring ON payments (next_occurrence_date) WHERE is_recurring = true AND deleted_at IS NULL;
CREATE INDEX idx_expenses_recurring ON expenses (next_occurrence_date) WHERE is_recurring = true AND deleted_at IS NULL;
