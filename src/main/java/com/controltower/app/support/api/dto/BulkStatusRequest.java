package com.controltower.app.support.api.dto;

import com.controltower.app.support.domain.Ticket;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Schema(description = "Request to update the status of multiple tickets at once")
public class BulkStatusRequest {

    @NotEmpty
    @Schema(description = "IDs of the tickets to update (max 100)", example = "[\"uuid-1\", \"uuid-2\"]")
    private List<UUID> ticketIds;

    @NotNull
    @Schema(description = "Target status to apply to all tickets", example = "RESOLVED")
    private Ticket.TicketStatus status;
}
