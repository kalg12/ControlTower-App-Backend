package com.controltower.app.support.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class TicketResponse {

    private final UUID         id;
    private final UUID         tenantId;
    private final UUID         clientId;
    private final UUID         branchId;
    private final String       title;
    private final String       description;
    private final String       priority;
    private final String       status;
    private final UUID         assigneeId;
    private final String              source;
    private final String              sourceRefId;
    private final Map<String, Object> posContext;
    private final List<String>        labels;
    private final int          commentsCount;
    private final Integer      estimatedMinutes;
    /** SLA deadline (null when no SLA attached). */
    private final Instant      slaDueAt;
    /** Whether the SLA has been breached. */
    private final Boolean      slaBreached;
    private final Instant      createdAt;
    private final Instant      updatedAt;
    private final Instant      deletedAt;
}
