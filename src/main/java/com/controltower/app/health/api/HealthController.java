package com.controltower.app.health.api;

import com.controltower.app.health.api.dto.*;
import com.controltower.app.health.application.HeartbeatService;
import com.controltower.app.health.application.HealthIncidentService;
import com.controltower.app.health.application.HealthQueryService;
import com.controltower.app.health.domain.HealthIncident;
import com.controltower.app.health.domain.HealthRule;
import com.controltower.app.health.domain.HealthRuleRepository;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import com.controltower.app.tenancy.domain.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/**
 * Health monitoring endpoints.
 * POST /heartbeat is public (auth via branchSlug — integration key future).
 */
@Tag(name = "Health", description = "Health monitoring — heartbeats, incidents, rules")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final HeartbeatService     heartbeatService;
    private final HealthQueryService   queryService;
    private final HealthIncidentService incidentService;
    private final HealthRuleRepository ruleRepository;

    // ── Public heartbeat ──────────────────────────────────────────────

    /**
     * Receives a heartbeat ping from a client system.
     * Public endpoint — authenticated via branchSlug (no JWT required).
     * Future: add API key header validation.
     */
    @Operation(summary = "Receive heartbeat", description = "Accepts a heartbeat ping from a client POS system identified by its branch slug. Public endpoint — no JWT required.")
    @PostMapping("/heartbeat/{branchSlug}")
    public ResponseEntity<ApiResponse<Void>> heartbeat(
            @PathVariable String branchSlug,
            @RequestBody(required = false) HeartbeatRequest request) {
        heartbeatService.processHeartbeat(branchSlug, request != null ? request : new HeartbeatRequest());
        return ResponseEntity.ok(ApiResponse.ok("Heartbeat received"));
    }

    // ── Dashboard overview ────────────────────────────────────────────

    @Operation(summary = "Get health overview", description = "Returns an aggregated health summary for all clients and their branches within the current tenant. Requires the 'health:read' permission.")
    @GetMapping("/clients")
    @PreAuthorize("hasAuthority('health:read')")
    public ResponseEntity<ApiResponse<List<BranchHealthSummary>>> getOverview() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(ApiResponse.ok(queryService.getClientOverview(tenantId)));
    }

    @Operation(summary = "Get branch health checks", description = "Returns a paginated history of health check results for a specific branch. Requires the 'health:read' permission.")
    @GetMapping("/branches/{branchId}")
    @PreAuthorize("hasAuthority('health:read')")
    public ResponseEntity<ApiResponse<PageResponse<HealthCheckResponse>>> getBranchChecks(
            @PathVariable UUID branchId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("checkedAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(queryService.getBranchChecks(branchId, pageable)));
    }

    // ── Incidents ─────────────────────────────────────────────────────

    @Operation(summary = "List health incidents", description = "Returns a paginated list of health incidents for the current tenant, ordered by most recent. Requires the 'health:read' permission.")
    @GetMapping("/incidents")
    @PreAuthorize("hasAuthority('health:read')")
    public ResponseEntity<ApiResponse<PageResponse<HealthIncidentResponse>>> getIncidents(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = TenantContext.getTenantId();
        PageRequest pageable = PageRequest.of(page, size, Sort.by("openedAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(queryService.getIncidents(tenantId, pageable)));
    }

    @Operation(summary = "Resolve incident", description = "Marks an open health incident as resolved. Requires the 'health:write' permission.")
    @PostMapping("/incidents/{incidentId}/resolve")
    @PreAuthorize("hasAuthority('health:write')")
    public ResponseEntity<ApiResponse<Void>> resolveIncident(@PathVariable UUID incidentId) {
        incidentService.resolve(incidentId);
        return ResponseEntity.ok(ApiResponse.ok("Incident resolved"));
    }

    // ── Rules ─────────────────────────────────────────────────────────

    @Operation(summary = "Create health rule", description = "Creates a new alert rule for a branch, specifying rule type, threshold, severity, and alert channel. Requires the 'health:write' permission.")
    @PostMapping("/rules")
    @PreAuthorize("hasAuthority('health:write')")
    public ResponseEntity<ApiResponse<Void>> createRule(@Valid @RequestBody HealthRuleRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        HealthRule rule = new HealthRule();
        rule.setTenantId(tenantId);
        rule.setBranchId(request.getBranchId());
        rule.setRuleType(request.getRuleType());
        rule.setThresholdValue(request.getThresholdValue());
        rule.setSeverity(request.getSeverity());
        rule.setAlertChannel(request.getAlertChannel());
        ruleRepository.save(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Rule created"));
    }
}
