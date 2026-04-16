package com.controltower.app.reports.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Schema(description = "Client ranked by ticket volume in the selected period")
@Getter
@Builder
public class TopClientRow {
    private final UUID   clientId;
    private final String clientName;
    private final long   ticketCount;
}
