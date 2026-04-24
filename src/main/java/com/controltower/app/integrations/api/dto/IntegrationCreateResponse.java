package com.controltower.app.integrations.api.dto;

public record IntegrationCreateResponse(
        IntegrationEndpointResponse endpoint,
        String generatedApiKey
) {}
