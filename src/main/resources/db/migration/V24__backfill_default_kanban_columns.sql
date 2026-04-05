-- Boards with no columns (e.g. created before default columns were persisted correctly): add the four workflow columns.
INSERT INTO board_columns (id, board_id, name, position, wip_limit, column_kind, created_at, updated_at)
SELECT gen_random_uuid(), b.id, x.name, x.pos, NULL, x.kind, NOW(), NOW()
FROM boards b
CROSS JOIN (
    VALUES
        ('To Do', 0, 'TODO'),
        ('In Progress', 1, 'IN_PROGRESS'),
        ('Done', 2, 'DONE'),
        ('History', 3, 'HISTORY')
) AS x(name, pos, kind)
WHERE b.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM board_columns c WHERE c.board_id = b.id);
