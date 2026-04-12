package com.controltower.app.integrations.infrastructure;

import com.controltower.app.health.application.HeartbeatService;
import com.controltower.app.health.domain.HealthCheck;
import com.controltower.app.integrations.domain.IntegrationEndpoint;
import com.controltower.app.integrations.domain.IntegrationEndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Periodically pulls health status from registered endpoints that expose a pullUrl.
 * GETs each endpoint's /health URL and maps the response to a heartbeat record.
 *
 * Expected POS /health response shape:
 * {
 *   "status": "ok" | "degraded" | "down",
 *   "uptimeSeconds": number,
 *   "timestamp": "ISO-8601",
 *   "checks": {
 *     "database": { "status": "up"|"down"|"warn" },
 *     "memory": { "heapUsedMb": number, "heapTotalMb": number, "usagePercent": number, "status": "up"|"warn" },
 *     "controlTower": { "status": "up"|"down"|"warn" }
 *   }
 * }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PullScheduler {

    private final IntegrationEndpointRepository endpointRepository;
    private final HeartbeatService              heartbeatService;

    private final RestClient restClient = RestClient.create();

    /** Every 60 seconds: pull-check all active endpoints that have a pullUrl. Runs immediately on startup. */
    @Scheduled(initialDelay = 0, fixedDelay = 60_000)
    public void pullAll() {
        List<IntegrationEndpoint> endpoints =
                endpointRepository.findByActiveAndDeletedAtIsNullAndPullUrlIsNotNull(true);

        if (endpoints.isEmpty()) return;

        log.debug("Pull-checking {} integration endpoint(s)", endpoints.size());
        endpoints.forEach(this::pullOne);
    }

    /** Triggers an immediate pull-check for a single endpoint by ID. No-op if not found/inactive/no URL. */
    public void pullEndpoint(UUID endpointId) {
        endpointRepository.findById(endpointId)
                .filter(ep -> ep.getDeletedAt() == null && ep.isActive() && ep.getPullUrl() != null)
                .ifPresent(this::pullOne);
    }

    @SuppressWarnings("unchecked")
    private void pullOne(IntegrationEndpoint endpoint) {
        if (endpoint.getClientBranchId() == null) return;

        long start = System.currentTimeMillis();
        try {
            var requestSpec = restClient.get().uri(endpoint.getPullUrl());
            if (endpoint.getApiKey() != null && !endpoint.getApiKey().isBlank()) {
                requestSpec = requestSpec.header("Authorization", "Bearer " + endpoint.getApiKey());
            }

            ResponseEntity<Map> response = requestSpec.retrieve().toEntity(Map.class);
            int latencyMs = (int) (System.currentTimeMillis() - start);

            // Map POS status field: "ok"→UP, "degraded"→DEGRADED, anything else→UP
            HealthCheck.HealthStatus status = HealthCheck.HealthStatus.UP;
            String version = endpoint.getContractVersion();

            if (response.getBody() != null) {
                Object posStatus = response.getBody().get("status");
                if ("degraded".equals(posStatus)) {
                    status = HealthCheck.HealthStatus.DEGRADED;
                } else if ("down".equals(posStatus)) {
                    status = HealthCheck.HealthStatus.DOWN;
                }
                Object v = response.getBody().get("version");
                if (v instanceof String s && !s.isBlank()) {
                    version = s;
                }
            }

            heartbeatService.processPullResult(endpoint.getClientBranchId(), status, latencyMs, version);
            log.debug("Pull-check {} for endpoint {} — latency {}ms", status, endpoint.getId(), latencyMs);

        } catch (Exception e) {
            // HTTP failure (connection refused, timeout, 4xx, 5xx) → record DOWN immediately
            log.warn("Pull check FAILED for endpoint {} ({}): {}", endpoint.getId(), endpoint.getPullUrl(), e.getMessage());
            heartbeatService.processFailedPull(endpoint.getClientBranchId(), e.getMessage());
        }
    }
}
