package com.controltower.app.clients.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ContactResponse {

    private final UUID   id;
    private final UUID   clientId;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String role;
    private final boolean primary;
    private final String notes;
    private final Instant createdAt;
}
