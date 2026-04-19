package com.controltower.app.kanban.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class WorkItemResponse {

    private final UUID id;
    private final CardResponse card;
    private final UUID boardId;
    private final String boardName;
    private final UUID columnId;
    private final String columnName;
    /** Matches {@link com.controltower.app.kanban.domain.BoardColumn.ColumnKind} or null for custom columns */
    private final String columnKind;
    private final UUID tenantId;
    private final String tenantName;
    private final List<String> assigneeNames;
    private final String checklistProgress;
    private final boolean overdue;
}
