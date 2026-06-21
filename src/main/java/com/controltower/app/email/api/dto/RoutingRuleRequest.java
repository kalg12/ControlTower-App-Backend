package com.controltower.app.email.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RoutingRuleRequest(
    @NotBlank String name,
    UUID aliasId,
    Integer priority,
    @NotNull List<Map<String, Object>> conditions,
    @NotNull List<Map<String, Object>> actions,
    String matchMode,
    Map<String, Object> schedule
) {}
