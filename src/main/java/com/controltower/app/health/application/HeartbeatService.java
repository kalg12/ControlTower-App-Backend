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
        check.setStatus(HealthCheck.HealthStatus.UP);
        check.setLatencyMs(request.getLatencyMs());
        check.setVersion(request.getVersion());
        check.setSource(HealthCheck.CheckSource.HEARTBEAT);
        if (request.getMetadata() != null) {
            check.setMetadata(request.getMetadata());
        }

        healthCheckRepository.save(check);
        log.debug("Heartbeat received from branch {} (slug: {})", branch.getId(), branchSlug);

        // Evaluate incident rules asynchronously
        incidentService.evaluateAfterCheck(branch, check);
    }
}
