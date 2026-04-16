package com.controltower.app.tenancy.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class TenantResponse {

    private final UUID id;
    private final String name;
    private final String slug;
    private final String status;
    private final String country;
    private final String timezone;
    private final String currency;
    private final Instant createdAt;
}
