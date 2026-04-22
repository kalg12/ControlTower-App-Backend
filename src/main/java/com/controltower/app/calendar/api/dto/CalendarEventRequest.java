package com.controltower.app.calendar.api.dto;

import com.controltower.app.calendar.domain.CalendarEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CalendarEventRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private CalendarEvent.EventType eventType;

    @NotNull
    private Instant startAt;

    @NotNull
    private Instant endAt;

    private UUID clientId = null;
    private UUID personId;
    private UUID branchId;
    private String notes;
    private String outcome;
    private CalendarEvent.ContactChannel contactChannel;
    private CalendarEvent.EventStatus status;
    private List<UUID> assigneeIds;
}
