package com.controltower.app.reports.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Schema(description = "Performance metrics for a single agent over the selected period")
@Getter
@Builder
public class AgentPerformanceRow {
    private final UUID   agentId;
    private final String agentName;
    private final long   assigned;
    private final long   resolved;
    /** Average resolution time in minutes (null if no resolved tickets). */
    private final Double avgMinutes;
    /** SLA compliance rate 0–100 (null if no SLA data). */
    private final Double slaRate;
}
