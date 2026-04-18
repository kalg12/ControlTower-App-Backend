package com.controltower.app.notifications.infrastructure;

import com.controltower.app.health.domain.HealthIncidentOpenedEvent;
import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.notifications.application.NotificationService;
import com.controltower.app.notifications.domain.Notification;
import com.controltower.app.shared.infrastructure.EmailService;
import com.controltower.app.support.domain.PosTicketChatEvent;
import com.controltower.app.support.domain.PosTicketReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    private List<UUID> usersWithPermission(UUID tenantId, String permission) {
        return userRepository.findByTenantIdAndPermission(tenantId, permission)
                .stream().map(u -> u.getId()).toList();
    }

    @Async
    @EventListener
    public void onHealthIncident(HealthIncidentOpenedEvent event) {
        log.info("Sending notification for health incident {}", event.getIncidentId());

        Notification.Severity severity = switch (event.getSeverity()) {
            case LOW      -> Notification.Severity.INFO;
            case MEDIUM   -> Notification.Severity.WARNING;
            case HIGH     -> Notification.Severity.ERROR;
            case CRITICAL -> Notification.Severity.CRITICAL;
        };

        notificationService.send(
                event.getTenantId(),
                "HEALTH_INCIDENT",
                "Health incident detected",
                event.getDescription(),
                severity,
                Map.of(
                    "incidentId", event.getIncidentId().toString(),
                    "branchId",   event.getBranchId().toString()
                ),
                usersWithPermission(event.getTenantId(), "health:read")
        );
    }

    @Async
    @EventListener
    public void onPosTicketChat(PosTicketChatEvent event) {
        log.info("Sending chat notification for POS ticket {}", event.getPosTicketId());
        notificationService.send(
                event.getTenantId(),
                "POS_CHAT",
                "Nuevo mensaje del POS",
                event.getSenderName() + " desde " + event.getBranchName() + ": " + event.getContent(),
                Notification.Severity.INFO,
                Map.of(
                    "ticketId",    event.getTicketId().toString(),
                    "posTicketId", event.getPosTicketId() != null ? event.getPosTicketId() : "",
                    "senderName",  event.getSenderName(),
                    "branchName",  event.getBranchName()
                ),
                usersWithPermission(event.getTenantId(), "ticket:read")
        );
    }

    @Async
    @EventListener
    public void onPosTicketReceived(PosTicketReceivedEvent event) {
        log.info("Sending notification for POS ticket {}", event.getPosTicketId());

        notificationService.send(
                event.getTenantId(),
                "POS_TICKET",
                "Nuevo ticket de soporte del POS",
                event.getSubmittedBy() + " desde " + event.getBranchName() + ": " + event.getTitle(),
                Notification.Severity.WARNING,
                Map.of(
                    "ticketId",    event.getTicketId().toString(),
                    "posTicketId", event.getPosTicketId() != null ? event.getPosTicketId() : "",
                    "branchId",    event.getBranchId() != null ? event.getBranchId().toString() : "",
                    "branchName",  event.getBranchName(),
                    "submittedBy", event.getSubmittedBy(),
                    "source",      "POS"
                ),
                usersWithPermission(event.getTenantId(), "ticket:read")
        );
    }
}
