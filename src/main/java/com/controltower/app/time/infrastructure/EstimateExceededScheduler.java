package com.controltower.app.time.infrastructure;

import com.controltower.app.kanban.domain.Card;
import com.controltower.app.kanban.domain.CardRepository;
import com.controltower.app.notifications.application.NotificationService;
import com.controltower.app.notifications.domain.Notification;
import com.controltower.app.support.domain.Ticket;
import com.controltower.app.support.domain.TicketRepository;
import com.controltower.app.time.domain.TimeEntry;
import com.controltower.app.time.domain.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EstimateExceededScheduler {

    private final TicketRepository    ticketRepository;
    private final CardRepository      cardRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void checkEstimates() {
        checkTickets();
        checkCards();
    }

    private void checkTickets() {
        List<Ticket> tickets = ticketRepository.findActiveWithEstimates();
        for (Ticket ticket : tickets) {
            long logged = timeEntryRepository.sumMinutesByEntityTypeAndEntityId(TimeEntry.EntityType.TICKET, ticket.getId());
            if (logged <= ticket.getEstimatedMinutes()) continue;

            notificationService.send(
                    ticket.getTenantId(),
                    "ESTIMATE_EXCEEDED",
                    "Estimación superada",
                    "El ticket \"" + ticket.getTitle() + "\" superó su estimación ("
                            + logged + " min registrados vs " + ticket.getEstimatedMinutes() + " estimados)",
                    Notification.Severity.WARNING,
                    Map.of("entityType", "TICKET", "entityId", ticket.getId().toString(),
                            "logged", logged, "estimated", ticket.getEstimatedMinutes()),
                    List.of(ticket.getAssigneeId()));
        }
    }

    private void checkCards() {
        List<Card> cards = cardRepository.findActiveWithEstimates();
        for (Card card : cards) {
            long logged = timeEntryRepository.sumMinutesByEntityTypeAndEntityId(TimeEntry.EntityType.CARD, card.getId());
            if (logged <= card.getEstimatedMinutes()) continue;

            UUID tenantId = card.getBoardColumn().getBoard().getTenantId();
            for (UUID assigneeId : card.getAssigneeIds()) {
                notificationService.send(
                        tenantId,
                        "ESTIMATE_EXCEEDED",
                        "Estimación superada",
                        "La tarea \"" + card.getTitle() + "\" superó su estimación ("
                                + logged + " min registrados vs " + card.getEstimatedMinutes() + " estimados)",
                        Notification.Severity.WARNING,
                        Map.of("entityType", "CARD", "entityId", card.getId().toString(),
                                "logged", logged, "estimated", card.getEstimatedMinutes()),
                        List.of(assigneeId));
            }
        }
    }
}
