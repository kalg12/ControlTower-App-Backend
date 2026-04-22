package com.controltower.app.reports.application;

import com.controltower.app.clients.domain.Client;
import com.controltower.app.clients.domain.ClientRepository;
import com.controltower.app.identity.domain.User;
import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.reports.api.dto.*;
import com.controltower.app.support.domain.TicketRepository;
import com.controltower.app.support.domain.TicketSlaRepository;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportsService {

    private final TicketRepository    ticketRepository;
    private final TicketSlaRepository slaRepository;
    private final UserRepository      userRepository;
    private final ClientRepository    clientRepository;

    private static final DateTimeFormatter DAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private Instant defaultFrom(Instant from) {
        return from != null ? from : Instant.now().minus(30, ChronoUnit.DAYS);
    }

    private Instant defaultTo(Instant to) {
        return to != null ? to : Instant.now();
    }

    // ── Tickets trend ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TicketTrendPoint> getTicketsTrend(Instant from, Instant to) {
        UUID tenantId = TenantContext.getTenantId();
        Map<String, Long> byDay = new LinkedHashMap<>();
        ticketRepository
            .findForReports(tenantId, defaultFrom(from), defaultTo(to))
            .forEach(ticket -> {
                String day = DAY_FMT.format(ticket.getCreatedAt());
                byDay.merge(day, 1L, Long::sum);
            });

        return byDay.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> TicketTrendPoint.builder().date(e.getKey()).count(e.getValue()).build())
            .collect(Collectors.toList());
    }

    // ── Agent performance ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AgentPerformanceRow> getAgentPerformance(Instant from, Instant to) {
        UUID tenantId = TenantContext.getTenantId();

        Map<UUID, String> nameMap = userRepository
            .findByTenantIdAndDeletedAtIsNull(tenantId, PageRequest.of(0, 500))
            .getContent()
            .stream()
            .collect(Collectors.toMap(User::getId, User::getFullName));

        // [assigned, resolved]
        Map<UUID, long[]> stats = new LinkedHashMap<>();
        ticketRepository
            .findForReports(tenantId, defaultFrom(from), defaultTo(to))
            .forEach(ticket -> {
                if (ticket.getAssigneeId() == null) return;
                long[] s = stats.computeIfAbsent(ticket.getAssigneeId(), k -> new long[]{0, 0});
                s[0]++;
                if (ticket.getStatus().name().equals("RESOLVED") || ticket.getStatus().name().equals("CLOSED")) {
                    s[1]++;
                }
            });

        return stats.entrySet().stream()
            .map(e -> {
                long[] s = e.getValue();
                return AgentPerformanceRow.builder()
                    .agentId(e.getKey())
                    .agentName(nameMap.getOrDefault(e.getKey(), e.getKey().toString().substring(0, 8)))
                    .assigned(s[0])
                    .resolved(s[1])
                    .avgMinutes(null)   // not available without resolvedAt field
                    .slaRate(null)
                    .build();
            })
            .sorted(Comparator.comparingLong(AgentPerformanceRow::getAssigned).reversed())
            .collect(Collectors.toList());
    }

    // ── Top clients ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TopClientRow> getTopClients(Instant from, Instant to, int limit) {
        UUID tenantId = TenantContext.getTenantId();

        Map<UUID, Long> counts = new LinkedHashMap<>();
        ticketRepository
            .findForReports(tenantId, defaultFrom(from), defaultTo(to))
            .forEach(ticket -> {
                if (ticket.getClientId() != null)
                    counts.merge(ticket.getClientId(), 1L, Long::sum);
            });

        // Build a client-name map for the IDs we found
        Map<UUID, String> nameMap = new HashMap<>();
        if (!counts.isEmpty()) {
            clientRepository
                .findByTenantIdAndDeletedAtIsNull(tenantId, PageRequest.of(0, 1000))
                .getContent()
                .forEach(c -> nameMap.put(c.getId(), c.getName()));
        }

        return counts.entrySet().stream()
            .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
            .limit(limit)
            .map(e -> TopClientRow.builder()
                .clientId(e.getKey())
                .clientName(nameMap.getOrDefault(e.getKey(), "Cliente " + e.getKey().toString().substring(0, 8)))
                .ticketCount(e.getValue())
                .build())
            .collect(Collectors.toList());
    }

    // ── SLA trend ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SlaTrendPoint> getSlaTrend(Instant from, Instant to) {
        UUID tenantId = TenantContext.getTenantId();

        Map<String, long[]> byDay = new LinkedHashMap<>();
        ticketRepository
            .findForReports(tenantId, defaultFrom(from), defaultTo(to))
            .forEach(ticket -> {
                String day = DAY_FMT.format(ticket.getCreatedAt());
                long[] pair = byDay.computeIfAbsent(day, k -> new long[]{0, 0});
                boolean breached = ticket.getSla() != null && ticket.getSla().isBreached();
                if (breached) pair[1]++; else pair[0]++;
            });

        return byDay.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> SlaTrendPoint.builder()
                .date(e.getKey())
                .ok(e.getValue()[0])
                .breached(e.getValue()[1])
                .build())
            .collect(Collectors.toList());
    }
}
