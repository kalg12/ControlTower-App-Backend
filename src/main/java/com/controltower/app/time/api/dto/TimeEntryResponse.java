package com.controltower.app.time.api.dto;

import com.controltower.app.time.domain.TimeEntry;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "A single work-log entry (timer or manual)")

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class TimeEntryResponse {
    private final UUID                 id;
    private final UUID                 userId;
    private final TimeEntry.EntityType entityType;
    private final UUID                 entityId;
    private final Instant              startedAt;
    private final Instant              endedAt;
    private final Integer              minutes;
    private final String               note;
    private final boolean              active;
    private final Instant              createdAt;
}
