package com.controltower.app.notifications.infrastructure;

import com.controltower.app.chat.domain.ChatConversationStartedEvent;
import com.controltower.app.chat.domain.ChatVisitorMessageReceivedEvent;
import com.controltower.app.chat.domain.ChatRatedEvent;
import com.controltower.app.clients.domain.ClientBranchRepository;
import com.controltower.app.health.domain.HealthIncidentOpenedEvent;
import com.controltower.app.health.domain.HealthIncidentResolvedEvent;
import com.controltower.app.identity.domain.User;
import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.notifications.application.NotificationService;
import com.controltower.app.notifications.domain.Notification;
import com.controltower.app.notifications.domain.NotificationPreferenceRepository;
import com.controltower.app.shared.infrastructure.EmailService;
import com.controltower.app.support.domain.PosTicketChatEvent;
import com.controltower.app.support.domain.PosTicketReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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
    private final ClientBranchRepository branchRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    private List<User> usersByPermission(UUID tenantId, String permission) {
        return userRepository.findByTenantIdAndPermission(tenantId, permission);
    }

    private List<UUID> usersWithPermission(UUID tenantId, String permission) {
        return usersByPermission(tenantId, permission).stream().map(User::getId).toList();
    }

    @EventListener
    public void onHealthIncident(HealthIncidentOpenedEvent event) {
        log.info("Sending notification for health incident {}", event.getIncidentId());

        Notification.Severity severity = switch (event.getSeverity()) {
            case LOW      -> Notification.Severity.INFO;
            case MEDIUM   -> Notification.Severity.WARNING;
            case HIGH     -> Notification.Severity.ERROR;
            case CRITICAL -> Notification.Severity.CRITICAL;
        };

        List<User> recipients = usersByPermission(event.getTenantId(), "health:read");

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
                recipients.stream().map(User::getId).toList()
        );

        String branchName = branchRepository.findById(event.getBranchId())
                .map(b -> b.getName())
                .orElse("sucursal desconocida");
        recipients.stream()
                .filter(u -> preferenceRepository.isEnabled(u.getId(), "HEALTH_INCIDENT"))
                .forEach(u -> emailService.sendHealthIncidentNotification(
                        u.getEmail(), u.getFullName(), branchName,
                        event.getSeverity().name(), event.getDescription()));
    }

    @EventListener
    public void onHealthIncidentResolved(HealthIncidentResolvedEvent event) {
        log.info("Sending notification for resolved health incident {}", event.getIncidentId());
        
        String resolvedByName = event.getResolvedBy() != null 
                ? userRepository.findById(event.getResolvedBy()).map(u -> u.getFullName()).orElse("unknown")
                : "System";
        
        String title = event.isAutoResolved() 
                ? "Health incident auto-resolved" 
                : "Health incident resolved";
        
        String body = event.getDescription() + " - Resolved by " + resolvedByName;
        
        notificationService.send(
                event.getTenantId(),
                "HEALTH_INCIDENT_RESOLVED",
                title,
                body,
                Notification.Severity.INFO,
                Map.of(
                    "incidentId", event.getIncidentId().toString(),
                    "branchId", event.getBranchId().toString()
                ),
                usersWithPermission(event.getTenantId(), "health:read")
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPosTicketReceived(PosTicketReceivedEvent event) {
        log.info("Sending notification for POS ticket {}", event.getPosTicketId());

        List<User> recipients = usersByPermission(event.getTenantId(), "ticket:read");

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
                recipients.stream().map(User::getId).toList()
        );

        recipients.stream()
                .filter(u -> preferenceRepository.isEnabled(u.getId(), "POS_TICKET"))
                .forEach(u -> emailService.sendPosTicketAlert(
                        u.getEmail(), u.getFullName(), event.getTitle(), event.getBranchName(),
                        event.getSubmittedBy(), event.getPriority().name(), event.getTicketId().toString()));
    }

    @EventListener
    public void onChatConversationStarted(ChatConversationStartedEvent event) {
        String visitorLabel = event.getVisitorName() != null ? event.getVisitorName()
                            : event.getVisitorEmail() != null ? event.getVisitorEmail()
                            : "Visitante anónimo";
        String source = event.getSource() != null ? event.getSource() : "web";

        notificationService.send(
                event.getTenantId(),
                "CHAT_CONVERSATION_STARTED",
                "Nuevo chat entrante",
                visitorLabel + " está esperando atención (via " + source + ")",
                Notification.Severity.INFO,
                Map.of(
                    "conversationId", event.getConversationId().toString(),
                    "source",         source
                ),
                usersWithPermission(event.getTenantId(), "chat:read")
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onChatVisitorMessageReceived(ChatVisitorMessageReceivedEvent event) {
        String visitorLabel = event.getVisitorName() != null && !event.getVisitorName().isBlank()
                ? event.getVisitorName()
                : "Visitante";
        String content = event.getContent() != null ? event.getContent().strip() : "";
        String preview = content.length() > 120 ? content.substring(0, 117) + "..." : content;
        List<UUID> recipients = event.getAssignedAgentId() != null
                ? List.of(event.getAssignedAgentId())
                : usersWithPermission(event.getTenantId(), "chat:read");

        notificationService.send(
                event.getTenantId(),
                "CHAT_MESSAGE_RECEIVED",
                "Nuevo mensaje de " + visitorLabel,
                preview.isBlank() ? "Tienes un nuevo mensaje en el chat" : preview,
                Notification.Severity.INFO,
                Map.of(
                    "conversationId", event.getConversationId().toString(),
                    "senderName", visitorLabel
                ),
                recipients
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onChatRated(ChatRatedEvent event) {
        String visitorLabel = event.getVisitorName() != null && !event.getVisitorName().isBlank()
                ? event.getVisitorName()
                : "El cliente";
        String comment = event.getComment() != null ? event.getComment().strip() : "";
        String preview = comment.length() > 160 ? comment.substring(0, 157) + "..." : comment;
        String body = visitorLabel + " calificó la atención con " + event.getRating() + "/5"
                + (preview.isBlank() ? "." : ": " + preview);
        List<UUID> recipients = event.getAssignedAgentId() != null
                ? List.of(event.getAssignedAgentId())
                : usersWithPermission(event.getTenantId(), "chat:read");

        notificationService.send(
                event.getTenantId(),
                "CHAT_RATED",
                event.getRating() <= 2 ? "Evaluación de chat requiere seguimiento" : "Nueva evaluación de chat",
                body,
                event.getRating() <= 2 ? Notification.Severity.WARNING : Notification.Severity.INFO,
                Map.of(
                        "conversationId", event.getConversationId().toString(),
                        "rating", event.getRating()
                ),
                recipients
        );
    }
}
