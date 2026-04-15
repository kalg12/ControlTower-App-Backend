package com.controltower.app.time.api.dto;

import com.controltower.app.time.domain.TimeEntry;
import lombok.Builder;
import lombok.Getter;

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
