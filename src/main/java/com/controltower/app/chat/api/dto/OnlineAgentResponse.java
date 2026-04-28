package com.controltower.app.chat.api.dto;

import java.util.UUID;

public record OnlineAgentResponse(
        UUID agentId,
        String name,
        String avatarUrl,
        long activeChats
) {}
