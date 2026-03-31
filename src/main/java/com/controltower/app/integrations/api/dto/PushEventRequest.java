package com.controltower.app.integrations.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class PushEventRequest {

    @NotNull(message = "endpointId is required")
    private UUID endpointId;

    @NotBlank(message = "eventType is required")
    private String eventType;

    private Map<String, Object> payload;
}
