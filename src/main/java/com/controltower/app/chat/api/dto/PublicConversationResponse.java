package com.controltower.app.chat.api.dto;

import java.util.UUID;

public record PublicConversationResponse(
        UUID conversationId,
        String status,
        String agentName,
        String agentAvatarUrl
) {}
