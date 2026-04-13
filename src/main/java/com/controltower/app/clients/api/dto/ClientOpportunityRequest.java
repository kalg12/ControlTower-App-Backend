package com.controltower.app.clients.api.dto;

import com.controltower.app.clients.domain.ClientOpportunity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class ClientOpportunityRequest {

    @NotBlank
    private String title;

    private String description;

    private Double value;

    private String currency;

    @NotNull
    private ClientOpportunity.OpportunityStage stage;

    private Integer probability;

    private UUID ownerId;

    private Instant expectedCloseDate;

    private ClientOpportunity.OpportunitySource source;

    // For closing as lost:
    private String lossReason;
}
