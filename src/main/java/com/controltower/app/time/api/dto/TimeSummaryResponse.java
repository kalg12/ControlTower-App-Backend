package com.controltower.app.time.api.dto;

import com.controltower.app.time.domain.TimeEntry;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.UUID;

@Schema(description = "Time summary for a ticket or card: estimated vs logged minutes")
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
