package com.controltower.app.integrations.infrastructure;

import com.controltower.app.health.application.HeartbeatService;
import com.controltower.app.health.domain.HealthCheck;
import com.controltower.app.integrations.domain.IntegrationEndpoint;
import com.controltower.app.integrations.domain.IntegrationEndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.time.Duration;
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
 *   "version": "1.0.0",
 *   "uptimeSeconds": number,
 *   "timestamp": "ISO-8601",
 *   "checks": {
 *     "database": { "status": "up"|"down"|"warn" },
 *     "memory": { "heapUsedMb": number, "heapTotalMb": number, "usagePercent": number, "status": "up"|"warn" },
 *     "controlTower": { "status": "up"|"down"|"warn" }
 *   }
 * }
 *
 * Architecture improvements:
 * - Retry with exponential backoff (2 attempts) to avoid false positives from transient network issues
 * - Error classification: distinguishes between connection refused, timeout, and HTTP errors
 * - Detailed logging for observability
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PullScheduler {

    private static final int MAX_RETRIES = 2;
    private static final long INITIAL_BACKOFF_MS = 2_000;
    private static final long MAX_BACKOFF_MS = 5_000;

    private final IntegrationEndpointRepository endpointRepository;
    private final HeartbeatService              heartbeatService;

    private final RestClient restClient = buildRestClient();

    /**
     * Creates a RestClient backed by SimpleClientHttpRequestFactory (HttpURLConnection).
     * This opens a fresh TCP connection for every request — no connection pooling.
     *
     * RestClient.create() uses JdkClientHttpRequestFactory (JDK 11 HttpClient) which
     * pools connections internally. Node.js closes keep-alive connections after ~5 s,
     * so the pooled connection is always stale when the scheduler fires 60 s later,
     * producing "I/O error … null" and a false DOWN check.
     */
    private static RestClient buildRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(factory).build();
    }

    /** Every 60 seconds: pull-check all active endpoints that have a pullUrl. Runs immediately on startup. */
    @Scheduled(initialDelay = 0, fixedDelay = 60_000)
    public void pullAll() {
        List<IntegrationEndpoint> endpoints =
                endpointRepository.findByActiveAndDeletedAtIsNullAndPullUrlIsNotNull(true);

        if (endpoints.isEmpty()) return;

        log.info("Pull-checking {} integration endpoint(s)", endpoints.size());
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
        Exception lastException = null;

        // Retry loop with exponential backoff
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                var requestSpec = restClient.get().uri(endpoint.getPullUrl());
                if (endpoint.getApiKey() != null && !endpoint.getApiKey().isBlank()) {
                    requestSpec = requestSpec.header("Authorization", "Bearer " + endpoint.getApiKey());
                }

                ResponseEntity<Map> response = requestSpec.retrieve().toEntity(Map.class);
                int latencyMs = (int) (System.currentTimeMillis() - start);

                // Map POS status field: "ok"→UP, "degraded"→DEGRADED, "down"→DOWN
                HealthCheck.HealthStatus status = HealthCheck.HealthStatus.UP;
                String version = endpoint.getContractVersion();
                String detail = null;

                if (response.getBody() != null) {
                    Object posStatus = response.getBody().get("status");
                    if ("degraded".equals(posStatus)) {
                        status = HealthCheck.HealthStatus.DEGRADED;
                    } else if ("down".equals(posStatus)) {
                        status = HealthCheck.HealthStatus.DOWN;
                    }

                    // Extract version if present
                    Object v = response.getBody().get("version");
                    if (v instanceof String s && !s.isBlank()) {
                        version = s;
                    }

                    // Extract detail from checks for observability
                    @SuppressWarnings("unchecked")
                    Map<String, Object> checks = (Map<String, Object>) response.getBody().get("checks");
                    if (checks != null) {
                        detail = buildDetailFromChecks(checks);
                    }
                }

                heartbeatService.processPullResult(endpoint.getClientBranchId(), status, latencyMs, version, detail);
                log.info("Pull-check {} for endpoint [{}] — latency={}ms version={}",
                        status, endpoint.getId(), latencyMs, version);

                // Success on first or second attempt — exit retry loop
                return;

            } catch (Exception e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    long backoff = Math.min(INITIAL_BACKOFF_MS * (long) Math.pow(2, attempt - 1), MAX_BACKOFF_MS);
                    log.warn("Pull check attempt {} failed for endpoint {} ({}): {} — retrying in {}ms",
                            attempt, endpoint.getId(), endpoint.getPullUrl(), e.getMessage(), backoff);
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Retry sleep interrupted for endpoint {}", endpoint.getId());
                        break;
                    }
                }
            }
        }

        // All retries exhausted — classify the error and record failure
        String errorMessage = classifyError(lastException);
        log.error("Pull check FAILED for endpoint {} ({}) after {} attempts: {}",
                endpoint.getId(), endpoint.getPullUrl(), MAX_RETRIES, errorMessage);
        heartbeatService.processFailedPull(endpoint.getClientBranchId(), errorMessage);
    }

    /**
     * Classifies the error to provide better observability about why the health check failed.
     * Categories: CONNECTION_REFUSED, TIMEOUT, HTTP_ERROR, UNKNOWN
     */
    private String classifyError(Exception e) {
        if (e == null) return "Unknown error";

        String msg = e.getMessage();
        if (msg == null) return e.getClass().getSimpleName();

        // Truncate to avoid storing huge stack traces
        if (msg.length() > 500) {
            msg = msg.substring(0, 500);
        }

        // Classify for better diagnostics
        if (e instanceof ResourceAccessException) {
            if (e.getCause() instanceof SocketTimeoutException) {
                return "TIMEOUT: " + msg;
            }
            if (msg.contains("Connection refused")) {
                return "CONNECTION_REFUSED: " + msg;
            }
            if (msg.contains("connect timed out") || msg.contains("read timed out")) {
                return "TIMEOUT: " + msg;
            }
        }

        return "HTTP_ERROR: " + msg;
    }

    /**
     * Builds a human-readable detail string from the health check sub-checks.
     * Example: "db=up, memory=warn(78%), ct=up"
     */
    @SuppressWarnings("unchecked")
    private String buildDetailFromChecks(Map<String, Object> checks) {
        StringBuilder sb = new StringBuilder();

        if (checks.containsKey("database")) {
            Map<String, Object> db = (Map<String, Object>) checks.get("database");
            sb.append("db=").append(db.get("status"));
        }

        if (checks.containsKey("memory")) {
            Map<String, Object> mem = (Map<String, Object>) checks.get("memory");
            if (sb.length() > 0) sb.append(", ");
            sb.append("memory=").append(mem.get("status"));
            if (mem.containsKey("usagePercent")) {
                sb.append("(").append(mem.get("usagePercent")).append("%)");
            }
        }

        if (checks.containsKey("controlTower")) {
            Map<String, Object> ct = (Map<String, Object>) checks.get("controlTower");
            if (sb.length() > 0) sb.append(", ");
            sb.append("ct=").append(ct.get("status"));
        }

        return sb.length() > 0 ? sb.toString() : null;
    }
}
