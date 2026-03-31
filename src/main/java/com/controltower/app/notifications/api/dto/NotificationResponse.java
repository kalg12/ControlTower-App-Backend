package com.controltower.app.notifications.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class NotificationResponse {

    private final UUID               id;
    private final String             type;
    private final String             title;
    private final String             body;
    private final String             severity;
    private final Map<String, Object> metadata;
    private final boolean            read;
    private final Instant            readAt;
    private final Instant            createdAt;
}
