package com.controltower.app.calendar.api.dto;

import com.controltower.app.calendar.domain.CalendarEvent;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CalendarEventResponse {
    private UUID id;
    private UUID tenantId;
    private String title;
    private String description;
    private CalendarEvent.EventType eventType;
    private Instant startAt;
    private Instant endAt;
    private UUID clientId;
    private String clientName;
    private UUID branchId;
    private CalendarEvent.EventStatus status;
    private String notes;
    private String outcome;
    private CalendarEvent.ContactChannel contactChannel;
    private UUID createdBy;
    private List<UUID> assigneeIds;
    private Instant createdAt;
    private Instant updatedAt;
}
