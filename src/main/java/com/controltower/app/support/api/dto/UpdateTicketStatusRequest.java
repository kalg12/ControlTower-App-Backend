package com.controltower.app.support.api.dto;

import com.controltower.app.support.domain.Ticket;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request to update a ticket status")
public class UpdateTicketStatusRequest {

    @NotNull
    @Schema(description = "Target status", example = "IN_PROGRESS")
    private Ticket.TicketStatus status;
}

