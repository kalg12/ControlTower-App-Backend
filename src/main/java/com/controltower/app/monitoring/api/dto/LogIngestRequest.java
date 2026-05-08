package com.controltower.app.monitoring.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class LogIngestRequest {

    @NotNull
    private UUID endpointId;

    @NotEmpty
    @Valid
    private List<LogEntryDto> entries;
}
