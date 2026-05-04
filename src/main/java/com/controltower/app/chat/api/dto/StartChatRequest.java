package com.controltower.app.chat.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StartChatRequest(
        @NotNull UUID tenantId,
        @NotBlank String visitorName,
        @NotBlank @Email String visitorEmail,
        String visitorId,
        String source
) {}
