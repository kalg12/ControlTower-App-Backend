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
    private final Instant createdAt;
}
