package com.controltower.app.clients.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class BranchResponse {

    private final UUID id;
    private final UUID clientId;
    private final UUID tenantId;
    private final String name;
    private final String address;
    private final String city;
    private final String country;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final String slug;
    private final String status;
    private final boolean isActive;
    private final String timezone;
    private final Instant createdAt;
}
