package com.controltower.app.integrations.infrastructure;

import com.controltower.app.health.api.dto.HeartbeatRequest;
import com.controltower.app.health.application.HeartbeatService;
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

    /** Every 5 minutes: pull-check all active endpoints that have a pullUrl. */
    @Scheduled(fixedDelay = 300_000)
    public void pullAll() {
        List<IntegrationEndpoint> endpoints =
                endpointRepository.findByActiveAndDeletedAtIsNullAndPullUrlIsNotNull(true);

        if (endpoints.isEmpty()) return;

        log.debug("Pull-checking {} integration endpoint(s)", endpoints.size());
        endpoints.forEach(this::pullOne);
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

            HeartbeatRequest req = new HeartbeatRequest();
            req.setLatencyMs(latencyMs);
            req.setVersion(endpoint.getContractVersion());

            // Extract version from response if available
            if (response.getBody() != null) {
                Object version = response.getBody().get("version");
                if (version instanceof String v && !v.isBlank()) {
                    req.setVersion(v);
                }
            }

            heartbeatService.processHeartbeatByBranchId(endpoint.getClientBranchId(), req);
            log.debug("Pull-check OK for endpoint {} — latency {}ms", endpoint.getId(), latencyMs);

        } catch (Exception e) {
            log.warn("Pull check failed for endpoint {} ({}): {}", endpoint.getId(), endpoint.getPullUrl(), e.getMessage());
        }
    }
}
