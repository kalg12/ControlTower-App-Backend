package com.controltower.app.tenancy.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingRequest {

    @NotBlank(message = "tenantName is required")
    private String tenantName;

    @NotBlank(message = "tenantSlug is required")
    private String tenantSlug;

    @NotBlank(message = "adminEmail is required")
    @Email(message = "adminEmail must be a valid email")
    private String adminEmail;

    @NotBlank(message = "adminPassword is required")
    private String adminPassword;

    @NotBlank(message = "adminFullName is required")
    private String adminFullName;
}
