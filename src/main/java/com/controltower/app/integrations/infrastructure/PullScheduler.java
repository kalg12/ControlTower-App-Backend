package com.controltower.app.integrations.infrastructure;

import com.controltower.app.health.api.dto.HeartbeatRequest;
import com.controltower.app.health.application.HeartbeatService;
import com.controltower.app.integrations.domain.IntegrationEndpoint;
import com.controltower.app.integrations.domain.IntegrationEndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Periodically pulls health status from registered endpoints that expose a pullUrl.
 * Simulates a heartbeat for each active endpoint that has a pull URL configured.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PullScheduler {

    private final IntegrationEndpointRepository endpointRepository;
    private final HeartbeatService              heartbeatService;

    /** Every 5 minutes: pull-check all active endpoints that have a pullUrl. */
    @Scheduled(fixedDelay = 300_000)
    public void pullAll() {
        List<IntegrationEndpoint> endpoints =
                endpointRepository.findByActiveAndDeletedAtIsNullAndPullUrlIsNotNull(true);

        if (endpoints.isEmpty()) return;

        log.debug("Pull-checking {} integration endpoint(s)", endpoints.size());
        endpoints.forEach(endpoint -> {
            if (endpoint.getClientBranchId() == null) return;
            try {
                HeartbeatRequest req = new HeartbeatRequest();
                req.setVersion(endpoint.getContractVersion());
                // In production this would perform an HTTP GET to pullUrl and map the response.
                heartbeatService.processHeartbeatByBranchId(endpoint.getClientBranchId(), req);
            } catch (Exception e) {
                log.warn("Pull check failed for endpoint {}: {}", endpoint.getId(), e.getMessage());
            }
        });
    }
}
