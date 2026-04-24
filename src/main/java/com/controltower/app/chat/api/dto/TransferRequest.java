package com.controltower.app.chat.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TransferRequest(@NotNull UUID toAgentId, String note) {}
