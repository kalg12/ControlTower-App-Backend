package com.controltower.app.health.domain;

import com.controltower.app.shared.events.DomainEvent;
import lombok.Getter;

import java.util.UUID;

/**
 * Published when a new health incident is opened.
 * The Support module listens to this event to auto-create a ticket.
 * The Notifications module listens to send an alert.
 */
@Getter
public class HealthIncidentOpenedEvent extends DomainEvent {

    private final UUID incidentId;
    private final UUID branchId;
    private final UUID tenantId;
    private final HealthIncident.Severity severity;
    private final String description;
    private final boolean autoCreated;

    public HealthIncidentOpenedEvent(HealthIncident incident) {
        this.incidentId  = incident.getId();
        this.branchId    = incident.getBranchId();
        this.tenantId    = incident.getTenantId();
        this.severity    = incident.getSeverity();
        this.description = incident.getDescription();
        this.autoCreated = incident.isAutoCreated();
    }

    @Override
    public String getEventType() {
        return "health.incident.opened";
    }
}
