package com.controltower.app.clients.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ClientInteractionResponse {

    private final UUID id;
    private final UUID clientId;
    private final UUID branchId;
    private final String branchName;
    private final UUID userId;
    private final String userName;
    private final String interactionType;
    private final String title;
    private final String description;
    private final Instant occurredAt;
    private final UUID ticketId;
    private final String outcome;
    private final Integer durationMinutes;
    private final Instant createdAt;
}
