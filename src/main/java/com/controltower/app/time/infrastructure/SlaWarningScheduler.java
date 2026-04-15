package com.controltower.app.time.infrastructure;

import com.controltower.app.notifications.application.NotificationService;
import com.controltower.app.notifications.domain.Notification;
import com.controltower.app.support.domain.TicketSla;
import com.controltower.app.support.domain.TicketSlaRepository;
import com.controltower.app.time.domain.SlaNotificationSent;
import com.controltower.app.time.domain.SlaNotificationSentRepository;
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
 * Sends proactive SLA warning notifications to assignees when a ticket's SLA
 * reaches 50%, 75%, and 90% of its total window.
 * Runs every 60 seconds; uses {@link SlaNotificationSent} to prevent duplicates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlaWarningScheduler {

    private static final short[] THRESHOLDS = {50, 75, 90};

    private final TicketSlaRepository           slaRepository;
    private final SlaNotificationSentRepository sentRepository;
    private final NotificationService           notificationService;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void checkSlaWarnings() {
        List<TicketSla> activeSlas = slaRepository.findAllActive();
        if (activeSlas.isEmpty()) return;

        Instant now = Instant.now();
        int notified = 0;

        for (TicketSla sla : activeSlas) {
            UUID ticketId  = sla.getTicket().getId();
            UUID tenantId  = sla.getTicket().getTenantId();
            UUID assigneeId = sla.getTicket().getAssigneeId();

            if (assigneeId == null) continue; // unassigned tickets skip warning

            Instant createdAt = sla.getTicket().getCreatedAt();
            long totalWindow  = Duration.between(createdAt, sla.getDueAt()).toMillis();
            if (totalWindow <= 0) continue;

            long elapsed = Duration.between(createdAt, now).toMillis();
            int percentConsumed = (int) Math.min((elapsed * 100L) / totalWindow, 100);

            for (short threshold : THRESHOLDS) {
                if (percentConsumed >= threshold
                        && !sentRepository.existsByTicketIdAndThreshold(ticketId, threshold)) {

                    // Send WebSocket notification to the assignee
                    String title = String.format("SLA al %d%% — %s", threshold, sla.getTicket().getTitle());
                    String body  = buildBody(threshold, sla.getDueAt(), now);

                    notificationService.send(
                            tenantId,
                            "SLA_WARNING",
                            title,
                            body,
                            severityFor(threshold),
                            Map.of("ticketId", ticketId.toString(),
                                   "threshold", threshold,
                                   "dueAt", sla.getDueAt().toString()),
                            List.of(assigneeId)
                    );

                    // Persist dedup record
                    SlaNotificationSent dedup = new SlaNotificationSent();
                    dedup.setTicketId(ticketId);
                    dedup.setThreshold(threshold);
                    sentRepository.save(dedup);

                    log.info("SLA warning sent: ticket={} threshold={}%", ticketId, threshold);
                    notified++;
                }
            }
        }

        if (notified > 0) log.info("SlaWarningScheduler: {} notification(s) sent", notified);
    }

    private static String buildBody(short threshold, Instant dueAt, Instant now) {
        long minutesLeft = Duration.between(now, dueAt).toMinutes();
        if (minutesLeft <= 0) {
            return String.format("SLA consumido al %d%%. El ticket ya venció.", threshold);
        }
        long hoursLeft = minutesLeft / 60;
        long minsLeft  = minutesLeft % 60;
        return String.format("SLA consumido al %d%%. Quedan %dh %dm para el vencimiento.",
                threshold, hoursLeft, minsLeft);
    }

    private static Notification.Severity severityFor(short threshold) {
        if (threshold >= 90) return Notification.Severity.ERROR;
        if (threshold >= 75) return Notification.Severity.WARNING;
        return Notification.Severity.INFO;
    }
}
