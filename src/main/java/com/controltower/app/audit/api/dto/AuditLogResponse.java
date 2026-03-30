package com.controltower.app.audit.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AuditLogResponse {

    private final UUID id;
    private final UUID tenantId;
    private final UUID userId;
    private final String action;
    private final String resourceType;
    private final String resourceId;
    private final String result;
    private final String ipAddress;
    private final String correlationId;
    private final Instant createdAt;
}
