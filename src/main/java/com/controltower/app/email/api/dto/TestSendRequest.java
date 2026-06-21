package com.controltower.app.email.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TestSendRequest(
    @NotNull UUID mailboxId,
    @NotBlank @Email String to,
    String subject,
    String bodyHtml
) {}
