package com.controltower.app.chat.api.dto;

import com.controltower.app.chat.domain.ConversationStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record ChatConversationResponse(
        UUID id,
        UUID tenantId,
        String visitorId,
        String visitorName,
        String visitorEmail,
        UUID agentId,
        String agentName,
        String agentAvatarUrl,
        ConversationStatus status,
        String source,
        long unreadCount,
        List<ChatMessageResponse> messages,
        Instant createdAt,
        Instant updatedAt,
        Instant closedAt
) {}
