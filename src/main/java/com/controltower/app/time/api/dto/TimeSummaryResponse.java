package com.controltower.app.time.api.dto;

import com.controltower.app.time.domain.TimeEntry;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class TimeSummaryResponse {
    private final UUID                 entityId;
    private final TimeEntry.EntityType entityType;
    /** Estimated minutes set on the ticket or card. Null when not defined. */
    private final Integer              estimatedMinutes;
    /** Sum of all stopped time entries (minutes). */
    private final int                  loggedMinutes;
    /** All entries including the active (running) one if present. */
    private final List<TimeEntryResponse> entries;
}
