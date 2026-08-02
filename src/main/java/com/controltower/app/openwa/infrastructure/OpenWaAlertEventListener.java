package com.controltower.app.openwa.infrastructure;

import com.controltower.app.health.domain.HealthIncidentOpenedEvent;
import com.controltower.app.health.domain.HealthIncidentResolvedEvent;
import com.controltower.app.shared.infrastructure.OpenWaService;
import com.controltower.app.support.domain.PosTicketReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Forwards domain events to the dev team WhatsApp group via OpenWA.
 * Each handler runs @Async so the alert never blocks the event publisher.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenWaAlertEventListener {

    private final OpenWaService openWaService;

    @Async
    @EventListener
    public void onHealthIncidentOpened(HealthIncidentOpenedEvent event) {
        String message = String.format(
                "🚨 ALERTA COMERZA POS\n\n" +
                "Tipo: Health Incident\n" +
                "Severidad: %s\n" +
                "Descripción: %s\n" +
                "Incidente: %s\n" +
                "Hora UTC: %s\n\n" +
                "Revisar en Control Tower.",
                event.getSeverity(),
                event.getDescription(),
                event.getIncidentId(),
                Instant.now()
        );
        openWaService.sendDevAlert(message);
    }

    @Async
    @EventListener
    public void onHealthIncidentResolved(HealthIncidentResolvedEvent event) {
        String resolution = event.getResolutionNote() != null && !event.getResolutionNote().isBlank()
                ? event.getResolutionNote()
                : "Sin nota";
        String message = String.format(
                "✅ RECUPERADO COMERZA POS\n\n" +
                "Descripción: %s\n" +
                "Resolución: %s\n" +
                "Auto-resuelto: %s\n" +
                "Hora UTC: %s\n\n" +
                "Sistema operando normalmente.",
                event.getDescription(),
                resolution,
                event.isAutoResolved() ? "Sí" : "No",
                Instant.now()
        );
        openWaService.sendDevAlert(message);
    }

    @Async
    @EventListener
    public void onPosTicketReceived(PosTicketReceivedEvent event) {
        String message = String.format(
                "🎫 NUEVO TICKET POS — Comerza\n\n" +
                "Sucursal: %s\n" +
                "Título: %s\n" +
                "Prioridad: %s\n" +
                "Enviado por: %s\n" +
                "Hora UTC: %s\n\n" +
                "Ver en Control Tower.",
                event.getBranchName(),
                event.getTitle(),
                event.getPriority(),
                event.getSubmittedBy(),
                Instant.now()
        );
        openWaService.sendDevAlert(message);
    }
}
