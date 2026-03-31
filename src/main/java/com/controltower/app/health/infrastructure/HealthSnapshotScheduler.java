package com.controltower.app.health.infrastructure;

import com.controltower.app.health.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Generates daily health snapshots at 01:00 UTC for all active branches.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthSnapshotScheduler {

    private final HealthCheckRepository    checkRepository;
    private final HealthSnapshotRepository snapshotRepository;
    private final HealthIncidentRepository incidentRepository;

    @Scheduled(cron = "0 0 1 * * *") // 01:00 UTC daily
    @Transactional
    public void generateDailySnapshots() {
        LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        Instant from = yesterday.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to   = from.plus(1, ChronoUnit.DAYS);

        List<UUID> activeBranches = checkRepository.findActiveBranchIds(from, to);
        log.info("Generating health snapshots for {} branches for date {}", activeBranches.size(), yesterday);

        for (UUID branchId : activeBranches) {
            if (snapshotRepository.existsByBranchIdAndSnapshotDate(branchId, yesterday)) {
                log.debug("Snapshot already exists for branch {} on {}", branchId, yesterday);
                continue;
            }

            List<HealthCheck> checks = checkRepository.findByBranchAndDateRange(branchId, from, to);
            if (checks.isEmpty()) {
                continue;
            }

            int totalChecks = checks.size();
            long upChecks   = checks.stream()
                    .filter(c -> c.getStatus() == HealthCheck.HealthStatus.UP)
                    .count();
            double uptimePct = (double) upChecks / totalChecks * 100.0;

            OptionalDouble avgLatency = checks.stream()
                    .filter(c -> c.getLatencyMs() != null)
                    .mapToInt(HealthCheck::getLatencyMs)
                    .average();

            // Count incidents opened on this day for this branch
            long incidentCount = incidentRepository.findByBranchIdOrderByOpenedAtDesc(
                    branchId, PageRequest.of(0, Integer.MAX_VALUE))
                    .stream()
                    .filter(i -> {
                        Instant opened = i.getOpenedAt();
                        return opened != null && !opened.isBefore(from) && opened.isBefore(to);
                    })
                    .count();

            // Determine tenantId from one of the checks
            UUID tenantId = checks.get(0).getTenantId();

            HealthSnapshot snapshot = new HealthSnapshot();
            snapshot.setTenantId(tenantId);
            snapshot.setBranchId(branchId);
            snapshot.setSnapshotDate(yesterday);
            snapshot.setUptimePercent(uptimePct);
            snapshot.setAvgLatencyMs(avgLatency.isPresent() ? avgLatency.getAsDouble() : null);
            snapshot.setCheckCount(totalChecks);
            snapshot.setIncidentCount((int) incidentCount);
            snapshotRepository.save(snapshot);

            log.debug("Saved snapshot for branch {} on {}: uptime={}%, checks={}, incidents={}",
                    branchId, yesterday, String.format("%.2f", uptimePct), totalChecks, incidentCount);
        }

        log.info("Health snapshot generation complete for {}", yesterday);
    }
}
