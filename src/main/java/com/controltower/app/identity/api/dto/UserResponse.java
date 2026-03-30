package com.controltower.app.identity.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class UserResponse {

    private final UUID id;
    private final UUID tenantId;
    private final String email;
    private final String fullName;
    private final String status;
    private final boolean superAdmin;
    private final Set<String> roles;
    private final Set<String> permissions;
    private final Instant createdAt;
}
