package com.controltower.app.identity.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class LoginResponse {

    private final String accessToken;
    private final String refreshToken;
    private final String tokenType = "Bearer";
    private final UUID userId;
    private final UUID tenantId;
    private final String email;
    private final String fullName;
}
