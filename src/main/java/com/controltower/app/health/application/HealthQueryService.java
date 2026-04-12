package com.controltower.app.health.application;

import com.controltower.app.clients.domain.ClientBranch;
import com.controltower.app.clients.domain.ClientBranchRepository;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HealthQueryService {

    private final HealthCheckRepository    checkRepository;
    private final HealthIncidentRepository incidentRepository;
    private final ClientBranchRepository   branchRepository;

    /** Returns the latest health check for each branch of a tenant (dashboard view). */
    @Transactional(readOnly = true)
    public List<BranchHealthSummary> getClientOverview(UUID tenantId) {
        List<HealthCheck> latest = checkRepository.findLatestPerBranch(tenantId);
        if (latest.isEmpty()) return List.of();

        Set<UUID> branchIds = latest.stream()
                .map(HealthCheck::getBranchId)
                .collect(Collectors.toSet());

        // One JOIN FETCH query — no N+1
        Map<UUID, ClientBranch> branchMap = branchRepository.findAllByIdsWithClient(branchIds)
                .stream()
                .collect(Collectors.toMap(ClientBranch::getId, b -> b));

        return latest.stream()
                .map(h -> toBranchSummary(h, branchMap.get(h.getBranchId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<HealthCheckResponse> getBranchChecks(UUID branchId, Pageable pageable) {
        Page<HealthCheck> page = checkRepository.findByBranchIdOrderByCheckedAtDesc(branchId, pageable);
        return PageResponse.from(page.map(this::toCheckResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<HealthIncidentResponse> getIncidents(UUID tenantId, Pageable pageable) {
        Page<HealthIncident> page = incidentRepository.findByTenantIdOrderByOpenedAtDesc(tenantId, pageable);
        if (page.isEmpty()) return PageResponse.from(page.map(i -> toIncidentResponse(i, null)));

        Set<UUID> branchIds = page.stream()
                .map(HealthIncident::getBranchId)
                .collect(Collectors.toSet());

        Map<UUID, ClientBranch> branchMap = branchRepository.findAllByIdsWithClient(branchIds)
                .stream()
                .collect(Collectors.toMap(ClientBranch::getId, b -> b));

        return PageResponse.from(page.map(i -> toIncidentResponse(i, branchMap.get(i.getBranchId()))));
    }

    // ── Mapping ───────────────────────────────────────────────────────

    private BranchHealthSummary toBranchSummary(HealthCheck h, ClientBranch branch) {
        long openIncidents = incidentRepository
                .findByBranchIdAndResolvedAtIsNull(h.getBranchId())
                .map(i -> 1L).orElse(0L);

        return BranchHealthSummary.builder()
                .branchId(h.getBranchId())
                .branchName(branch != null ? branch.getName() : null)
                .clientName(branch != null ? branch.getClient().getName() : null)
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

    private HealthIncidentResponse toIncidentResponse(HealthIncident i, ClientBranch branch) {
        return HealthIncidentResponse.builder()
                .id(i.getId())
                .branchId(i.getBranchId())
                .branchName(branch != null ? branch.getName() : null)
                .severity(i.getSeverity().name())
                .description(i.getDescription())
                .openedAt(i.getOpenedAt())
                .resolvedAt(i.getResolvedAt())
                .open(i.isOpen())
                .autoCreated(i.isAutoCreated())
                .build();
    }
}
