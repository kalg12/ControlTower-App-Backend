package com.controltower.app.chat.api.dto;

import com.controltower.app.chat.domain.SenderType;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID conversationId,
        SenderType senderType,
        UUID senderId,
        String senderName,
        String senderAvatarUrl,
        String content,
        String attachmentUrl,
        boolean isRead,
        Instant createdAt
) {}
