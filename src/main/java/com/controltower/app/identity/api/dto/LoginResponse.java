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

    /** Present only when 2FA is required. Client must call /auth/2fa/verify with this token. */
    private final String mfaToken;

    /** True when 2FA is enabled and the client must complete MFA verification. */
    private final boolean requiresMfa;
}
