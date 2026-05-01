package com.controltower.app.support.infrastructure;

import com.controltower.app.health.domain.HealthIncident;
import com.controltower.app.health.domain.HealthIncidentOpenedEvent;
import com.controltower.app.support.application.TicketService;
import com.controltower.app.support.domain.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HealthIncidentListener {

    private final TicketService ticketService;

    @EventListener
    public void onIncidentOpened(HealthIncidentOpenedEvent event) {
        log.info("Auto-creating ticket for health incident {} on branch {}",
                event.getIncidentId(), event.getBranchId());

        Ticket.Priority priority = mapSeverity(event.getSeverity());
        String title = "Health incident: " + event.getDescription();

        ticketService.createFromIncident(
                event.getTenantId(),
                event.getBranchId(),
                title,
                priority,
                event.getIncidentId().toString()
        );
    }

    private Ticket.Priority mapSeverity(HealthIncident.Severity severity) {
        return switch (severity) {
            case LOW      -> Ticket.Priority.LOW;
            case MEDIUM   -> Ticket.Priority.MEDIUM;
            case HIGH     -> Ticket.Priority.HIGH;
            case CRITICAL -> Ticket.Priority.CRITICAL;
        };
    }
}
