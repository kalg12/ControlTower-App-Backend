package com.controltower.app.time.application;

import com.controltower.app.support.domain.TicketSlaRepository;
import com.controltower.app.support.domain.TicketRepository;
import com.controltower.app.tenancy.domain.TenantContext;
import com.controltower.app.time.api.dto.TimeAnalyticsResponse;
import com.controltower.app.time.domain.TimeEntry;
import com.controltower.app.time.domain.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimeAnalyticsService {

    private final TimeEntryRepository timeEntryRepository;
    private final TicketSlaRepository slaRepository;
    private final TicketRepository    ticketRepository;

    /**
     * Returns aggregated time metrics for the current tenant over the given period.
     * Default period (when both from/to are null): last 30 days.
     */
    @Transactional(readOnly = true)
    public TimeAnalyticsResponse getAnalytics(Instant from, Instant to) {
        UUID tenantId = TenantContext.getTenantId();

        if (from == null) from = Instant.now().minus(30, ChronoUnit.DAYS);
        if (to   == null) to   = Instant.now();

        // Total entries and minutes logged
        var page = timeEntryRepository.findByTenantAndPeriod(tenantId, from, to,
                PageRequest.of(0, Integer.MAX_VALUE));
        long totalEntries       = page.getTotalElements();
        long totalLoggedMinutes = page.getContent().stream()
                .filter(e -> e.getMinutes() != null)
                .mapToLong(TimeEntry::getMinutes)
                .sum();

        // Average resolution time (minutes) for tickets that have time entries
        double avgResolutionMinutes = page.getContent().stream()
                .filter(e -> e.getEntityType() == TimeEntry.EntityType.TICKET && e.getMinutes() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        TimeEntry::getEntityId,
                        java.util.stream.Collectors.summingInt(TimeEntry::getMinutes)
                ))
                .values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        // SLA compliance rate
        long totalTickets    = ticketRepository.countByTenantIdAndDeletedAtIsNull(tenantId);
        long breachedTickets = slaRepository.countActiveBreachedByTenant(tenantId);
        double slaComplianceRate = totalTickets == 0 ? 100.0
                : Math.round(((totalTickets - breachedTickets) * 100.0) / totalTickets * 100.0) / 100.0;

        // Top users by logged minutes
        List<TimeAnalyticsResponse.UserTimeEntry> topUsers = timeEntryRepository
                .sumMinutesPerUser(tenantId, from)
                .stream()
                .limit(10)
                .map(row -> TimeAnalyticsResponse.UserTimeEntry.builder()
                        .userId((UUID) row[0])
                        .totalMinutes(((Number) row[1]).longValue())
                        .build())
                .toList();

        return TimeAnalyticsResponse.builder()
                .avgResolutionMinutes(avgResolutionMinutes)
                .slaComplianceRate(slaComplianceRate)
                .totalEntries(totalEntries)
                .totalLoggedMinutes(totalLoggedMinutes)
                .topUsers(topUsers)
                .build();
    }
}
