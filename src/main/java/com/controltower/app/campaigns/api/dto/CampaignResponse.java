package com.controltower.app.campaigns.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CampaignResponse {

    private final UUID    id;
    private final UUID    tenantId;
    private final String  name;
    private final String  type;
    private final String  status;
    private final String  subject;
    private final String  body;
    private final String  targetAudience;
    private final int     sentCount;
    private final Double  openRate;
    private final Instant scheduledAt;
    private final Instant sentAt;
    private final Instant createdAt;
    private final Instant updatedAt;
}
