package com.controltower.app.clients.api.dto;

import com.controltower.app.clients.domain.ClientInteraction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class ClientInteractionRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private ClientInteraction.InteractionType interactionType;

    private UUID branchId;

    private Instant occurredAt;

    private UUID ticketId;

    private String outcome;

    private Integer durationMinutes;
}
