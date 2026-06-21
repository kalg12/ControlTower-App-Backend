package com.controltower.app.email.api.dto;

import com.controltower.app.email.domain.EmailAlias;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AliasResponse(
    UUID id,
    UUID tenantId,
    UUID mailboxId,
    String name,
    String alias,
    UUID departmentId,
    boolean active,
    List<String> forwardTo,
    Instant createdAt
) {
    public static AliasResponse from(EmailAlias a) {
        return new AliasResponse(
            a.getId(), a.getTenantId(), a.getMailboxId(),
            a.getName(), a.getAlias(), a.getDepartmentId(), a.isActive(),
            a.getForwardTo() != null ? List.of(a.getForwardTo()) : List.of(),
            a.getCreatedAt()
        );
    }
}
