package com.controltower.app.health.application;

import com.controltower.app.clients.domain.ClientBranch;
import com.controltower.app.clients.domain.ClientBranchRepository;
import com.controltower.app.health.api.dto.HeartbeatRequest;
import com.controltower.app.health.domain.HealthCheck;
import com.controltower.app.health.domain.HealthCheckRepository;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Processes incoming heartbeat pings from client systems.
 * After saving the check, triggers incident evaluation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeartbeatService {

    private final HealthCheckRepository healthCheckRepository;
    private final ClientBranchRepository branchRepository;
    private final HealthIncidentService incidentService;

    @Transactional
    public void processHeartbeat(String branchSlug, HeartbeatRequest request) {
        ClientBranch branch = branchRepository.findBySlugAndDeletedAtIsNull(branchSlug)
                .orElseThrow(() -> new ResourceNotFoundException("ClientBranch", branchSlug));

        HealthCheck check = new HealthCheck();
        check.setTenantId(branch.getTenant().getId());
        check.setBranchId(branch.getId());
        check.setStatus(resolveHeartbeatStatus(request));
        check.setLatencyMs(request.getLatencyMs());
        check.setVersion(request.getVersion());
        check.setSource(HealthCheck.CheckSource.HEARTBEAT);
        if (request.getMetadata() != null) {
            check.setMetadata(request.getMetadata());
        }

        check = healthCheckRepository.saveAndFlush(check);
        log.debug("Heartbeat received from branch {} (slug: {})", branch.getId(), branchSlug);

        incidentService.evaluateAfterCheck(branch, check);
    }

    private HealthCheck.HealthStatus resolveHeartbeatStatus(HeartbeatRequest request) {
        if (request == null) {
            return HealthCheck.HealthStatus.UP;
        }

        String rawStatus = request.getStatus();
        if ((rawStatus == null || rawStatus.isBlank()) && request.getMetadata() != null) {
            rawStatus = request.getMetadata();
        }
        if (rawStatus == null || rawStatus.isBlank()) {
            return HealthCheck.HealthStatus.UP;
        }

        String normalized = rawStatus.trim().toUpperCase();
        try {
            return HealthCheck.HealthStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            log.debug("Unknown heartbeat status payload '{}', defaulting to UP", rawStatus);
            return HealthCheck.HealthStatus.UP;
        }
    }

    /** Called by PullScheduler — resolves branch directly by UUID. */
    @Transactional
    public void processHeartbeatByBranchId(UUID branchId, HeartbeatRequest request) {
        ClientBranch branch = branchRepository.findById(branchId)
                .filter(b -> b.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("ClientBranch", branchId));

        HealthCheck check = new HealthCheck();
        check.setTenantId(branch.getTenant().getId());
        check.setBranchId(branch.getId());
        check.setStatus(HealthCheck.HealthStatus.UP);
        check.setLatencyMs(request.getLatencyMs());
        check.setVersion(request.getVersion());
        check.setSource(HealthCheck.CheckSource.PULL);
        check = healthCheckRepository.saveAndFlush(check);
        incidentService.evaluateAfterCheck(branch, check);
    }

    /**
     * Called by PullScheduler on a successful HTTP pull.
     * Saves a check with the real status reported by the POS
     * (ok→UP, degraded→DEGRADED, down→DOWN) instead of always UP.
     */
    @Transactional
    public void processPullResult(UUID branchId, HealthCheck.HealthStatus status,
                                   Integer latencyMs, String version) {
        processPullResult(branchId, status, latencyMs, version, null);
    }

    /**
     * Called by PullScheduler on a successful HTTP pull with optional detail info.
     * Saves a check with the real status reported by the POS
     * (ok→UP, degraded→DEGRADED, down→DOWN) instead of always UP.
     */
    @Transactional
    public void processPullResult(UUID branchId, HealthCheck.HealthStatus status,
                                   Integer latencyMs, String version, String detail) {
        ClientBranch branch = branchRepository.findById(branchId)
                .filter(b -> b.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("ClientBranch", branchId));

        HealthCheck check = new HealthCheck();
        check.setTenantId(branch.getTenant().getId());
        check.setBranchId(branch.getId());
        check.setStatus(status);
        check.setLatencyMs(latencyMs);
        check.setVersion(version);
        check.setSource(HealthCheck.CheckSource.PULL);
        if (detail != null && !detail.isBlank()) {
            check.setErrorMessage(detail);
        }
        check = healthCheckRepository.saveAndFlush(check);
        incidentService.evaluateAfterCheck(branch, check);
    }

    /**
     * Called by PullScheduler when the HTTP call to the POS fails entirely
     * (connection refused, timeout, 5xx response).
     * Records a DOWN check so the frontend reflects the real state immediately.
     */
    @Transactional
    public void processFailedPull(UUID branchId, String errorMessage) {
        ClientBranch branch = branchRepository.findById(branchId)
                .filter(b -> b.getDeletedAt() == null)
                .orElse(null);
        if (branch == null) {
            log.warn("processFailedPull: branch {} not found or deleted — skipping", branchId);
            return;
        }

        HealthCheck check = new HealthCheck();
        check.setTenantId(branch.getTenant().getId());
        check.setBranchId(branch.getId());
        check.setStatus(HealthCheck.HealthStatus.DOWN);
        check.setSource(HealthCheck.CheckSource.PULL);
        // Truncate to avoid storing huge stack traces
        if (errorMessage != null && errorMessage.length() > 500) {
            errorMessage = errorMessage.substring(0, 500);
        }
        check.setErrorMessage(errorMessage);
        check = healthCheckRepository.saveAndFlush(check);

        log.warn("DOWN check recorded for branch {} — pull failed: {}", branchId, errorMessage);
        incidentService.evaluateAfterCheck(branch, check);
    }
}
