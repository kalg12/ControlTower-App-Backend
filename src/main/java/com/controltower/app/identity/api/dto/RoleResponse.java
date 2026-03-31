package com.controltower.app.identity.api.dto;

import com.controltower.app.identity.domain.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
public class RoleResponse {

    private final UUID id;
    private final UUID tenantId;
    private final String name;
    private final String code;
    private final String description;
    private final boolean system;
    private final Set<String> permissions;
    private final Instant createdAt;

    public static RoleResponse from(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .tenantId(role.getTenant() != null ? role.getTenant().getId() : null)
                .name(role.getName())
                .code(role.getCode())
                .description(role.getDescription())
                .system(role.isSystem())
                .permissions(role.getPermissions().stream()
                        .map(p -> p.getCode())
                        .collect(Collectors.toSet()))
                .createdAt(role.getCreatedAt())
                .build();
    }
}
