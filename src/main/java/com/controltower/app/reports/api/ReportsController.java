package com.controltower.app.reports.api;

import com.controltower.app.reports.api.dto.*;
import com.controltower.app.reports.application.ReportsService;
import com.controltower.app.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Analytics and reporting endpoints scoped to the current tenant")
@SecurityRequirement(name = "bearerAuth")
public class ReportsController {

    private final ReportsService reportsService;

    @GetMapping("/tickets-trend")
    @Operation(summary = "Tickets created per day", description = "Returns daily ticket volume for the selected period (default: last 30 days).")
    public ResponseEntity<ApiResponse<List<TicketTrendPoint>>> getTicketsTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(ApiResponse.ok(reportsService.getTicketsTrend(from, to)));
    }

    @GetMapping("/agent-performance")
    @Operation(summary = "Agent performance metrics", description = "Returns per-agent ticket assignment and resolution counts for the selected period.")
    public ResponseEntity<ApiResponse<List<AgentPerformanceRow>>> getAgentPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(ApiResponse.ok(reportsService.getAgentPerformance(from, to)));
    }

    @GetMapping("/top-clients")
    @Operation(summary = "Top clients by ticket volume", description = "Returns the clients that opened the most tickets in the selected period.")
    public ResponseEntity<ApiResponse<List<TopClientRow>>> getTopClients(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(reportsService.getTopClients(from, to, limit)));
    }

    @GetMapping("/sla-trend")
    @Operation(summary = "SLA compliance trend", description = "Returns daily counts of tickets with SLA met vs. breached for the selected period.")
    public ResponseEntity<ApiResponse<List<SlaTrendPoint>>> getSlaTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(ApiResponse.ok(reportsService.getSlaTrend(from, to)));
    }
}
