package com.controltower.app.activity.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UserActivityResponse {

    private final UUID id;
    private final UUID userId;
    private final String userName;
    private final String userEmail;
    private final String routePath;
    private final String pageTitle;
    private final Integer durationSeconds;
    private final String fullUrl;
    private final String sessionId;
    private final String ipAddress;
    private final Instant visitedAt;
}
