package com.controltower.app.kanban.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class BoardResponse {
    private final UUID id;
    private final UUID tenantId;
    private final String name;
    private final String description;
    private final String visibility;
    private final UUID createdBy;
    private final UUID clientId;
    private final Instant createdAt;
    private final Instant updatedAt;
    /** Populated only on GET /boards/{id}; null on list. */
    private final List<ColumnResponse> columns;
}
