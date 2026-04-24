package com.controltower.app.chat.api.dto;

import com.controltower.app.chat.domain.SenderType;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ChatMessagePayload(
        String type,             // MESSAGE or TYPING or SYSTEM or STATUS_CHANGED
        UUID id,
        UUID conversationId,
        SenderType senderType,
        UUID senderId,
        String senderName,
        String senderAvatarUrl,
        String content,
        String attachmentUrl,
        boolean isRead,
        Instant createdAt,
        String conversationStatus  // for STATUS_CHANGED events
) {}
