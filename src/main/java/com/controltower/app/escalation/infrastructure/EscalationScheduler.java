package com.controltower.app.escalation.infrastructure;

import com.controltower.app.escalation.domain.EscalationRule;
import com.controltower.app.escalation.domain.EscalationRuleRepository;
import com.controltower.app.identity.domain.TenantRepository;
import com.controltower.app.notifications.application.NotificationService;
import com.controltower.app.notifications.domain.Notification;
import com.controltower.app.support.domain.Ticket;
import com.controltower.app.support.domain.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Runs every 15 minutes. For each tenant's escalation rules, finds tickets whose
 * last update is older than the configured threshold and marks them as escalated.
 * Sends an in-app notification to the assignee (if set) and supervisor channel.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EscalationScheduler {

    private final EscalationRuleRepository ruleRepository;
    private final TicketRepository         ticketRepository;
    private final TenantRepository         tenantRepository;
    private final NotificationService      notificationService;

    @Scheduled(fixedDelay = 15 * 60_000) // every 15 minutes
    @Transactional
    public void checkEscalations() {
        List<UUID> tenantIds = tenantRepository.findAll().stream()
                .filter(t -> t.getDeletedAt() == null)
                .map(t -> t.getId())
                .toList();

        int escalated = 0;
        for (UUID tenantId : tenantIds) {
            List<EscalationRule> rules = ruleRepository.findByTenantId(tenantId);
            if (rules.isEmpty()) continue;

            for (EscalationRule rule : rules) {
                Ticket.Priority priority = Ticket.Priority.valueOf(rule.getPriority());
                Instant threshold = Instant.now().minus(Duration.ofHours(rule.getHours()));

                List<Ticket> stale = ticketRepository.findStaleTickets(tenantId, priority, threshold);
                for (Ticket ticket : stale) {
                    ticket.setEscalatedAt(Instant.now());
                    // Bump priority by one level if not already CRITICAL
                    ticket.escalate();
                    ticketRepository.save(ticket);

                    if (ticket.getAssigneeId() != null) {
                        notificationService.send(
                                tenantId,
                                "TICKET_ESCALATED",
                                "Ticket escalado: " + ticket.getTitle(),
                                "El ticket lleva más de " + rule.getHours() + "h sin actualización y fue escalado automáticamente.",
                                Notification.Severity.WARNING,
                                Map.of("ticketId", ticket.getId().toString()),
                                List.of(ticket.getAssigneeId())
                        );
                    }
                    escalated++;
                }
            }
        }

        if (escalated > 0) log.info("EscalationScheduler: {} ticket(s) escalated", escalated);
    }
}
