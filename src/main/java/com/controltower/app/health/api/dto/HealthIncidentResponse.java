package com.controltower.app.health.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

// durationSeconds: seconds between openedAt and resolvedAt (or now if still open)

@Getter
@Builder
public class HealthIncidentResponse {

    private final UUID id;
    private final UUID branchId;
    private final String branchName;
    private final String severity;
    private final String description;
    private final Instant openedAt;
    private final Instant resolvedAt;
    private final boolean open;
    private final boolean autoCreated;
    private final long durationSeconds;
}
