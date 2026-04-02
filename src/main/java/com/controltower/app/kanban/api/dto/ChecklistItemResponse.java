package com.controltower.app.kanban.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ChecklistItemResponse {
    private final UUID id;
    private final UUID cardId;
    private final String text;
    private final boolean completed;
    private final int position;
    private final Instant createdAt;
}
