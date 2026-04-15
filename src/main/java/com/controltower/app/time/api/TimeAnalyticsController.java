package com.controltower.app.time.api;

import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.time.api.dto.TimeAnalyticsResponse;
import com.controltower.app.time.application.TimeAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Tag(name = "Time Analytics", description = "Aggregate time-tracking metrics and SLA compliance reports")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/analytics/time")
@RequiredArgsConstructor
public class TimeAnalyticsController {

    private final TimeAnalyticsService analyticsService;

    @Operation(
        summary = "Get time analytics for the current tenant",
        description = "Returns avg resolution time, SLA compliance rate, total logged minutes, and top users. Defaults to last 30 days."
    )
    @GetMapping
    @PreAuthorize("hasAuthority('tickets:read')")
    public ResponseEntity<ApiResponse<TimeAnalyticsResponse>> getAnalytics(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(ApiResponse.success(
                "Time analytics", analyticsService.getAnalytics(from, to)));
    }
}
