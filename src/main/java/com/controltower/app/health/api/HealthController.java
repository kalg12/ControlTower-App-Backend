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

/**
 * Health monitoring endpoints.
 * POST /heartbeat is public (auth via branchSlug — integration key future).
 */
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
    @PostMapping("/heartbeat/{branchSlug}")
    public ResponseEntity<ApiResponse<Void>> heartbeat(
            @PathVariable String branchSlug,
            @RequestBody(required = false) HeartbeatRequest request) {
        heartbeatService.processHeartbeat(branchSlug, request != null ? request : new HeartbeatRequest());
        return ResponseEntity.ok(ApiResponse.ok("Heartbeat received"));
    }

    // ── Dashboard overview ────────────────────────────────────────────

    @GetMapping("/clients")
    @PreAuthorize("hasAuthority('health:read')")
    public ResponseEntity<ApiResponse<List<BranchHealthSummary>>> getOverview() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(ApiResponse.ok(queryService.getClientOverview(tenantId)));
    }

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

    @GetMapping("/incidents")
    @PreAuthorize("hasAuthority('health:read')")
    public ResponseEntity<ApiResponse<PageResponse<HealthIncidentResponse>>> getIncidents(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = TenantContext.getTenantId();
        PageRequest pageable = PageRequest.of(page, size, Sort.by("openedAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(queryService.getIncidents(tenantId, pageable)));
    }

    @PostMapping("/incidents/{incidentId}/resolve")
    @PreAuthorize("hasAuthority('health:write')")
    public ResponseEntity<ApiResponse<Void>> resolveIncident(@PathVariable UUID incidentId) {
        incidentService.resolve(incidentId);
        return ResponseEntity.ok(ApiResponse.ok("Incident resolved"));
    }

    // ── Rules ─────────────────────────────────────────────────────────

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
