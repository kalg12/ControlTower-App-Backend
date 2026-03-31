package com.controltower.app.support.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Schema(description = "Request to assign multiple tickets to a single user")
public class BulkAssignRequest {

    @NotEmpty
    @Schema(description = "IDs of the tickets to assign (max 100)")
    private List<UUID> ticketIds;

    @NotNull
    @Schema(description = "User ID to assign all tickets to")
    private UUID assigneeId;
}
