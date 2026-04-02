package com.controltower.app.campaigns.api.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CampaignUpdateRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 500)
    private String subject;

    private String body;

    @Size(max = 255)
    private String targetAudience;

    private String scheduledAt;
}
