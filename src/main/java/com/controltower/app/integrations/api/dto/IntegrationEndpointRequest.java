package com.controltower.app.integrations.api.dto;

import com.controltower.app.integrations.domain.IntegrationEndpoint;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class IntegrationEndpointRequest {

    private UUID clientBranchId;

    private String name;

    private IntegrationEndpoint.EndpointType type;

    private String pullUrl;
    private String apiKey;
    private int    heartbeatIntervalSeconds = 300;
    private String contractVersion;
    private Map<String, Object> metadata;
}
