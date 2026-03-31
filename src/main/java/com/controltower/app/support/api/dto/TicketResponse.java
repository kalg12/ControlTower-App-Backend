package com.controltower.app.support.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class TicketResponse {

    private final UUID    id;
    private final UUID    tenantId;
    private final UUID    clientId;
    private final UUID    branchId;
    private final String  title;
    private final String  description;
    private final String  priority;
    private final String  status;
    private final UUID    assigneeId;
    private final String  source;
    private final String  sourceRefId;
    private final Instant createdAt;
    private final Instant updatedAt;
}
