package com.controltower.app.monitoring.api;

import com.controltower.app.monitoring.api.dto.LogIngestRequest;
import com.controltower.app.monitoring.api.dto.RemoteLogResponse;
import com.controltower.app.monitoring.application.RemoteLogService;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import com.controltower.app.tenancy.domain.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Tag(name = "Monitoring", description = "Remote log ingestion and query for integrated external systems")
@RestController
@RequestMapping("/api/v1/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final RemoteLogService remoteLogService;

    /**
     * Receives a batch of log entries from an external system (e.g. POS).
     * Public endpoint — authenticated via X-Api-Key tied to a registered IntegrationEndpoint.
     */
    @Operation(summary = "Ingest remote logs",
               description = "Receives batched log entries from external systems. Authenticated via X-Api-Key.")
    @PostMapping("/logs")
    public ResponseEntity<ApiResponse<Void>> ingestLogs(
            @Valid @RequestBody LogIngestRequest request,
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey) {
        remoteLogService.ingest(request, apiKey);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "List remote logs",
               description = "Returns paginated remote logs for the current tenant. Supports filtering by level, service, business name, and date range.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('integration:read')")
    public ResponseEntity<ApiResponse<PageResponse<RemoteLogResponse>>> list(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "50")  int size,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String businessName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(ApiResponse.ok(
                remoteLogService.list(TenantContext.getTenantId(), level, service, businessName, from, to, page, size)));
    }
}
