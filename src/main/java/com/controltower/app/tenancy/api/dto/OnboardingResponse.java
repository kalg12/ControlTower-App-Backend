package com.controltower.app.tenancy.api.dto;

import com.controltower.app.identity.api.dto.UserResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class OnboardingResponse {

    private final TenantResponse tenant;
    private final UserResponse user;
    private final UUID licenseId;
}
