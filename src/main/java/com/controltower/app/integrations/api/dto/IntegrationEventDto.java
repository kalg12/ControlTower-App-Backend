package com.controltower.app.integrations.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class IntegrationEventDto {
    private UUID   id;
    private String eventType;
    private Instant receivedAt;
    private Instant processedAt;
    private Map<String, Object> payload;
}
