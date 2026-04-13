package com.controltower.app.clients.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ClientOpportunityResponse {

    private final UUID id;
    private final UUID clientId;
    private final String clientName;
    private final UUID branchId;
    private final String title;
    private final String description;
    private final Double value;
    private final String currency;
    private final String stage;
    private final Integer probability;
    private final UUID ownerId;
    private final String ownerName;
    private final Instant expectedCloseDate;
    private final Instant closedDate;
    private final String lossReason;
    private final String source;
    private final Instant createdAt;
}
