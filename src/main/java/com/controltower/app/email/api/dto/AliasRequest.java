package com.controltower.app.email.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AliasRequest(
    @NotNull UUID mailboxId,
    @NotBlank String name,
    @NotBlank String alias,
    UUID departmentId,
    List<String> forwardTo
) {}
