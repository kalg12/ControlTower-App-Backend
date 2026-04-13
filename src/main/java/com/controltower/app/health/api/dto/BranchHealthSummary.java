package com.controltower.app.health.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class BranchHealthSummary {

    private final UUID branchId;
    private final String branchName;
    private final String clientName;
    private final String status;
    private final Integer latencyMs;
    private final String version;
    private final Instant lastCheckedAt;
    private final long openIncidents;
    /** Non-null only when status is DOWN — contains the error from the failed pull attempt. */
    private final String errorMessage;
}
