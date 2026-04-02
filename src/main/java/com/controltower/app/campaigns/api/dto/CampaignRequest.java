package com.controltower.app.campaigns.api.dto;

import com.controltower.app.campaigns.domain.Campaign;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CampaignRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    private Campaign.CampaignType type;

    @Size(max = 500)
    private String subject;

    @NotBlank
    private String body;

    @Size(max = 255)
    private String targetAudience;

    private String scheduledAt; // ISO-8601, parsed in service
}
