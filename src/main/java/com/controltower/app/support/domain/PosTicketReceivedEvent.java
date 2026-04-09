package com.controltower.app.support.domain;

import com.controltower.app.shared.events.DomainEvent;
import lombok.Getter;

import java.util.UUID;

/**
 * Published when a POS system submits a support ticket via the integration endpoint.
 * The Notifications module listens to send a real-time alert to CT operators.
 */
@Getter
public class PosTicketReceivedEvent extends DomainEvent {

    private final UUID              tenantId;
    private final UUID              ticketId;
    private final String            posTicketId;
    private final String            title;
    private final UUID              branchId;
    private final String            branchName;
    private final String            submittedBy;
    private final Ticket.Priority   priority;

    public PosTicketReceivedEvent(Ticket ticket, String branchName, String submittedBy) {
        this.tenantId    = ticket.getTenantId();
        this.ticketId    = ticket.getId();
        this.posTicketId = ticket.getSourceRefId();
        this.title       = ticket.getTitle();
        this.branchId    = ticket.getBranchId();
        this.branchName  = branchName != null ? branchName : "";
        this.submittedBy = submittedBy != null ? submittedBy : "";
        this.priority    = ticket.getPriority();
    }

    @Override
    public String getEventType() {
        return "pos.ticket.received";
    }
}
