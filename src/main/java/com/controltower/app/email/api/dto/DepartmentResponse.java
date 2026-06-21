package com.controltower.app.email.api.dto;

import com.controltower.app.email.domain.Department;

import java.time.Instant;
import java.util.UUID;

public record DepartmentResponse(
    UUID id,
    UUID tenantId,
    String name,
    String description,
    String color,
    String icon,
    int slaHours,
    boolean active,
    Instant createdAt
) {
    public static DepartmentResponse from(Department d) {
        return new DepartmentResponse(
            d.getId(), d.getTenantId(), d.getName(), d.getDescription(),
            d.getColor(), d.getIcon(), d.getSlaHours(), d.isActive(), d.getCreatedAt()
        );
    }
}
