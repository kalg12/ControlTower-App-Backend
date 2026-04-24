package com.controltower.app.chat.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record SendMessageRequest(
        @NotBlank String content,
        UUID conversationId
) {}
