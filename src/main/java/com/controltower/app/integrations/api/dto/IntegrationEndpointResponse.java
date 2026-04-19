package com.controltower.app.integrations.api.dto;

import com.controltower.app.integrations.domain.IntegrationEndpoint;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class IntegrationEndpointResponse {
    private UUID   id;
    private UUID   tenantId;
    private UUID   clientBranchId;
    private UUID   clientId;
    private String clientName;
    private String branchName;
    private String name;
    private IntegrationEndpoint.EndpointType type;
    private String pullUrl;
    private int    heartbeatIntervalSeconds;
    private String contractVersion;
    private Map<String, Object> metadata;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
