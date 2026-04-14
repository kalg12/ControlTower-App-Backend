package com.controltower.app.activity.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UserActivityResponse {

    private final UUID    id;
    private final UUID    userId;
    private final String  userName;
    private final String  userEmail;

    /** NAVIGATION or ACTION */
    private final String  eventType;

    /** For ACTION events: machine-readable action code (e.g. CARD_MOVED) */
    private final String  actionName;

    /** For ACTION events: domain entity type (e.g. KanbanCard) */
    private final String  entityType;

    /** For ACTION events: UUID of the affected entity */
    private final String  entityId;

    /** Human-readable description of what happened */
    private final String  description;

    /** For NAVIGATION events */
    private final String  routePath;
    private final String  pageTitle;
    private final Integer durationSeconds;
    private final String  fullUrl;

    private final String  sessionId;
    private final String  ipAddress;
    private final Instant visitedAt;
}
