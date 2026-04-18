package com.controltower.app.support.infrastructure;

import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.notifications.application.NotificationService;
import com.controltower.app.notifications.domain.Notification;
import com.controltower.app.support.domain.TicketSla;
import com.controltower.app.support.domain.TicketSlaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlaScheduler {

    private final TicketSlaRepository slaRepository;
    private final NotificationService notificationService;
    private final UserRepository      userRepository;

    /** Every 5 minutes: mark SLAs whose due_at has passed + send TICKET_SLA_BREACHED. */
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void checkBreaches() {
        List<TicketSla> overdue = slaRepository.findBreachedUnmarked(Instant.now());
        if (overdue.isEmpty()) return;

        log.warn("Marking {} SLA breach(es)", overdue.size());
        overdue.forEach(sla -> {
            sla.markBreached();
            slaRepository.save(sla);

            UUID tenantId  = sla.getTicket().getTenantId();
            UUID assigneeId = sla.getTicket().getAssigneeId();

            List<UUID> recipients = new ArrayList<>();
            if (assigneeId != null) recipients.add(assigneeId);
            userRepository.findByTenantIdAndPermission(tenantId, "ticket:write")
                    .forEach(u -> { if (!recipients.contains(u.getId())) recipients.add(u.getId()); });

            if (!recipients.isEmpty()) {
                notificationService.send(
                        tenantId,
                        "TICKET_SLA_BREACHED",
                        "SLA incumplido",
                        "El SLA del ticket \"" + sla.getTicket().getTitle() + "\" ha sido incumplido",
                        Notification.Severity.CRITICAL,
                        Map.of("ticketId", sla.getTicket().getId().toString()),
                        recipients);
            }
        });
    }
}
