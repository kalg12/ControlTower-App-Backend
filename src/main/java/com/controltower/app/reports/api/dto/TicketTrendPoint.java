package com.controltower.app.reports.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "Ticket volume for a single day")
@Getter
@Builder
public class TicketTrendPoint {
    private final String date;   // yyyy-MM-dd
    private final long   count;
}
