package com.controltower.app.chat.api.dto;

import java.util.UUID;

public record StartChatResponse(UUID conversationId, UUID visitorToken) {}
