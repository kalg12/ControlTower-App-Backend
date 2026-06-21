package com.controltower.app.email.api.dto;

import com.controltower.app.email.domain.EmailRoutingRule;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RoutingRuleResponse(
    UUID id,
    UUID tenantId,
    String name,
    UUID aliasId,
    int priority,
    List<Map<String, Object>> conditions,
    List<Map<String, Object>> actions,
    String matchMode,
    Map<String, Object> schedule,
    boolean active,
    Instant createdAt
) {
    public static RoutingRuleResponse from(EmailRoutingRule r) {
        return new RoutingRuleResponse(
            r.getId(), r.getTenantId(), r.getName(), r.getAliasId(),
            r.getPriority(), r.getConditions(), r.getActions(),
            r.getMatchMode(), r.getSchedule(), r.isActive(), r.getCreatedAt()
        );
    }
}
