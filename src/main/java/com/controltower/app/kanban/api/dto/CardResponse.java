package com.controltower.app.kanban.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class CardResponse {
    private final UUID id;
    private final UUID columnId;
    private final String title;
    private final String description;
    private final Set<UUID> assigneeIds;
    private final LocalDate dueDate;
    private final String priority;
    private final int position;
    private final String[] labels;
    private final List<ChecklistItemResponse> checklist;
    private final Integer estimatedMinutes;
    private final UUID attendedBy;
    private final Instant attendedAt;
    private final boolean wasOverdue;
    private final UUID clientId;
    private final Instant createdAt;
    private final Instant updatedAt;
}
