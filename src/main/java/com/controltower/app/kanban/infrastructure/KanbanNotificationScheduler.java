package com.controltower.app.kanban.infrastructure;

import com.controltower.app.kanban.domain.Card;
import com.controltower.app.kanban.domain.CardRepository;
import com.controltower.app.notifications.application.NotificationService;
import com.controltower.app.notifications.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KanbanNotificationScheduler {

    private final CardRepository      cardRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void checkDueDates() {
        LocalDate today = LocalDate.now();

        notifyDueSoon(today.plusDays(1));
        notifyOverdue(today);
    }

    private void notifyDueSoon(LocalDate dueDate) {
        List<Card> cards = cardRepository.findByDueDateAndHasAssignees(dueDate);
        for (Card card : cards) {
            UUID tenantId = card.getBoardColumn().getBoard().getTenantId();
            for (UUID assigneeId : card.getAssigneeIds()) {
                notificationService.send(
                        tenantId,
                        "CARD_DUE_SOON",
                        "Tarea por vencer mañana",
                        "La tarea \"" + card.getTitle() + "\" vence mañana (" + dueDate + ")",
                        Notification.Severity.WARNING,
                        Map.of("cardId", card.getId().toString(), "dueDate", dueDate.toString()),
                        List.of(assigneeId));
            }
        }
        if (!cards.isEmpty()) log.info("CARD_DUE_SOON sent for {} card(s)", cards.size());
    }

    private void notifyOverdue(LocalDate today) {
        List<Card> cards = cardRepository.findOverdueWithAssignees(today);
        for (Card card : cards) {
            UUID tenantId = card.getBoardColumn().getBoard().getTenantId();
            for (UUID assigneeId : card.getAssigneeIds()) {
                notificationService.send(
                        tenantId,
                        "CARD_OVERDUE",
                        "Tarea vencida",
                        "La tarea \"" + card.getTitle() + "\" venció el " + card.getDueDate(),
                        Notification.Severity.ERROR,
                        Map.of("cardId", card.getId().toString(), "dueDate", card.getDueDate().toString()),
                        List.of(assigneeId));
            }
        }
        if (!cards.isEmpty()) log.info("CARD_OVERDUE sent for {} card(s)", cards.size());
    }
}
