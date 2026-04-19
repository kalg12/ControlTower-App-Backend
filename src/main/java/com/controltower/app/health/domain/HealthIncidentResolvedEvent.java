package com.controltower.app.health.domain;

import com.controltower.app.shared.events.DomainEvent;
import lombok.Getter;

import java.util.UUID;

/**
 * Published when a health incident is resolved.
 * The Notifications module listens to send an alert to relevant users.
 */
@Getter
public class HealthIncidentResolvedEvent extends DomainEvent {

    private final UUID incidentId;
    private final UUID branchId;
    private final UUID tenantId;
    private final String description;
    private final UUID resolvedBy;
    private final String resolutionNote;
    private final boolean autoResolved;

    public HealthIncidentResolvedEvent(HealthIncident incident, UUID resolvedBy) {
        this.incidentId = incident.getId();
        this.branchId = incident.getBranchId();
        this.tenantId = incident.getTenantId();
        this.description = incident.getDescription();
        this.resolvedBy = resolvedBy;
        this.resolutionNote = incident.getResolutionNote();
        this.autoResolved = resolvedBy == null;
    }

    @Override
    public String getEventType() {
        return "health.incident.resolved";
    }
}