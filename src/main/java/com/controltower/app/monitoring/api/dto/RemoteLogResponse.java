package com.controltower.app.monitoring.api.dto;

import com.controltower.app.monitoring.domain.RemoteLog;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Value
@Builder
public class RemoteLogResponse {

    UUID    id;
    UUID    tenantId;
    UUID    endpointId;
    String  level;
    String  serviceName;
    String  message;
    String  stackTrace;
    String  businessName;
    String  source;
    Map<String, Object> metadata;
    Instant receivedAt;

    public static RemoteLogResponse from(RemoteLog log) {
        return RemoteLogResponse.builder()
                .id(log.getId())
                .tenantId(log.getTenantId())
                .endpointId(log.getEndpointId())
                .level(log.getLevel() != null ? log.getLevel().name() : null)
                .serviceName(log.getServiceName())
                .message(log.getMessage())
                .stackTrace(log.getStackTrace())
                .businessName(log.getBusinessName())
                .source(log.getSource())
                .metadata(log.getMetadata())
                .receivedAt(log.getReceivedAt())
                .build();
    }
}
