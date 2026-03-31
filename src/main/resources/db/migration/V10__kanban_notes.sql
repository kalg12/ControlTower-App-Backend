-- ─────────────────────────────────────────────────────────────────────────────
-- V10: Kanban Boards + Notes
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE boards (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    visibility  VARCHAR(20)  NOT NULL DEFAULT 'TEAM',
    created_by  UUID        REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_boards_tenant ON boards (tenant_id) WHERE deleted_at IS NULL;

CREATE TABLE board_columns (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id    UUID        NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    position    INT         NOT NULL DEFAULT 0,
    wip_limit   INT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_board_columns_board ON board_columns (board_id, position);

CREATE TABLE cards (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    column_id   UUID        NOT NULL REFERENCES board_columns(id) ON DELETE CASCADE,
    title       VARCHAR(500) NOT NULL,
    description TEXT,
    assignee_id UUID        REFERENCES users(id) ON DELETE SET NULL,
    due_date    DATE,
    priority    VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    position    INT         NOT NULL DEFAULT 0,
    labels      TEXT[],
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_cards_column   ON cards (column_id, position)   WHERE deleted_at IS NULL;
CREATE INDEX idx_cards_assignee ON cards (assignee_id)           WHERE deleted_at IS NULL;

CREATE TABLE checklist_items (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id     UUID        NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    text        VARCHAR(500) NOT NULL,
    completed   BOOLEAN     NOT NULL DEFAULT FALSE,
    position    INT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_checklist_items_card ON checklist_items (card_id, position);

CREATE TABLE notes (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    author_id   UUID        REFERENCES users(id)            ON DELETE SET NULL,
    title       VARCHAR(500) NOT NULL,
    content     TEXT,
    linked_to   VARCHAR(50),
    linked_id   UUID,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_notes_tenant ON notes (tenant_id)                       WHERE deleted_at IS NULL;
CREATE INDEX idx_notes_linked ON notes (tenant_id, linked_to, linked_id) WHERE deleted_at IS NULL;
