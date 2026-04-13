package com.controltower.app.clients.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ClientResponse {

    private final UUID id;
    private final UUID tenantId;
    private final String name;
    private final String legalName;
    private final String taxId;
    private final String country;
    private final String status;
    private final String notes;
    private final String website;
    private final String industry;
    private final String segment;
    private final UUID accountOwnerId;
    private final String accountOwnerName;
    private final Integer healthScore;
    private final Double totalRevenue;
    private final long contactCount;
    private final long branchCount;
    private final long openTicketsCount;
    private final long openOpportunitiesCount;
    private final Instant createdAt;
    private final Instant lastInteractionAt;
}
