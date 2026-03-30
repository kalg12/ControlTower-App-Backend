package com.controltower.app.health.application;

import com.controltower.app.health.api.dto.BranchHealthSummary;
import com.controltower.app.health.api.dto.HealthCheckResponse;
import com.controltower.app.health.api.dto.HealthIncidentResponse;
import com.controltower.app.health.domain.*;
import com.controltower.app.shared.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HealthQueryService {

    private final HealthCheckRepository    checkRepository;
    private final HealthIncidentRepository incidentRepository;

    /** Returns the latest health check for each branch of a tenant (dashboard view). */
    @Transactional(readOnly = true)
    public List<BranchHealthSummary> getClientOverview(UUID tenantId) {
        List<HealthCheck> latest = checkRepository.findLatestPerBranch(tenantId);
        return latest.stream().map(this::toBranchSummary).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<HealthCheckResponse> getBranchChecks(UUID branchId, Pageable pageable) {
        Page<HealthCheck> page = checkRepository.findByBranchIdOrderByCheckedAtDesc(branchId, pageable);
        return PageResponse.from(page.map(this::toCheckResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<HealthIncidentResponse> getIncidents(UUID tenantId, Pageable pageable) {
        Page<HealthIncident> page = incidentRepository.findByTenantIdOrderByOpenedAtDesc(tenantId, pageable);
        return PageResponse.from(page.map(this::toIncidentResponse));
    }

    // ── Mapping ───────────────────────────────────────────────────────

    private BranchHealthSummary toBranchSummary(HealthCheck h) {
        long openIncidents = incidentRepository
                .findByBranchIdAndResolvedAtIsNull(h.getBranchId())
                .map(i -> 1L).orElse(0L);

        return BranchHealthSummary.builder()
                .branchId(h.getBranchId())
                .status(h.getStatus().name())
                .latencyMs(h.getLatencyMs())
                .version(h.getVersion())
                .lastCheckedAt(h.getCheckedAt())
                .openIncidents(openIncidents)
                .build();
    }

    private HealthCheckResponse toCheckResponse(HealthCheck h) {
        return HealthCheckResponse.builder()
                .id(h.getId())
                .branchId(h.getBranchId())
                .status(h.getStatus().name())
                .latencyMs(h.getLatencyMs())
                .errorMessage(h.getErrorMessage())
                .version(h.getVersion())
                .source(h.getSource().name())
                .checkedAt(h.getCheckedAt())
                .build();
    }

    private HealthIncidentResponse toIncidentResponse(HealthIncident i) {
        return HealthIncidentResponse.builder()
                .id(i.getId())
                .branchId(i.getBranchId())
                .severity(i.getSeverity().name())
                .description(i.getDescription())
                .openedAt(i.getOpenedAt())
                .resolvedAt(i.getResolvedAt())
                .open(i.isOpen())
                .autoCreated(i.isAutoCreated())
                .build();
    }
}
