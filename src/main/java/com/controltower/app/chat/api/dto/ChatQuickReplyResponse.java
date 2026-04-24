package com.controltower.app.chat.api.dto;

import java.util.UUID;

public record ChatQuickReplyResponse(UUID id, String shortcut, String content) {}
