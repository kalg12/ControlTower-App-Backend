package com.controltower.app.time.api.dto;

import com.controltower.app.time.domain.TimeEntry;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class StartTimerRequest {

    @NotNull(message = "entityType is required")
    private TimeEntry.EntityType entityType;

    @NotNull(message = "entityId is required")
    private UUID entityId;
}
