package com.controltower.app.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Schema(description = "A user with their current open-ticket count (workload indicator)")
@Getter
@Builder
public class UserWorkloadResponse {
    private final UUID   id;
    private final String fullName;
    private final String email;
    private final long   openTickets;
}
