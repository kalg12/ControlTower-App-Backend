package com.controltower.app.kanban.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ColumnResponse {
    private final UUID id;
    private final UUID boardId;
    private final String name;
    private final int position;
    private final Integer wipLimit;
    private final List<CardResponse> cards;
    private final Instant createdAt;
}
