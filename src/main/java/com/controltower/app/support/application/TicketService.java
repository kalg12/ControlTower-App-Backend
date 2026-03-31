package com.controltower.app.support.application;

import com.controltower.app.shared.annotation.Audited;
import com.controltower.app.shared.exception.ControlTowerException;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.support.api.dto.*;
import com.controltower.app.support.domain.*;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository    ticketRepository;
    private final TicketSlaRepository slaRepository;

    // SLA windows by priority (hours)
    private static final int SLA_LOW      = 48;
    private static final int SLA_MEDIUM   = 24;
    private static final int SLA_HIGH     = 8;
    private static final int SLA_CRITICAL = 2;

    @Transactional(readOnly = true)
    public Page<TicketResponse> listTickets(
            Ticket.TicketStatus status, UUID assigneeId, UUID clientId, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        return ticketRepository.findFiltered(tenantId, status, assigneeId, clientId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(UUID ticketId) {
        return toResponse(resolveTicket(ticketId));
    }

    @Transactional
    @Audited(action = "TICKET_CREATED", resource = "Ticket")
    public TicketResponse createTicket(CreateTicketRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        Ticket ticket = new Ticket();
        ticket.setTenantId(tenantId);
        ticket.setClientId(request.getClientId());
        ticket.setBranchId(request.getBranchId());
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        if (request.getPriority() != null) ticket.setPriority(request.getPriority());

        ticketRepository.save(ticket);
        attachSla(ticket);
        return toResponse(ticketRepository.save(ticket));
    }

    /** Called internally (no TenantContext) when creating from a health incident. */
    @Transactional
    public TicketResponse createFromIncident(
            UUID tenantId, UUID branchId, String title,
            Ticket.Priority priority, String sourceRefId) {
        Ticket ticket = new Ticket();
        ticket.setTenantId(tenantId);
        ticket.setBranchId(branchId);
        ticket.setTitle(title);
        ticket.setPriority(priority);
        ticket.setSource(Ticket.TicketSource.HEALTH_ALERT);
        ticket.setSourceRefId(sourceRefId);
        ticketRepository.save(ticket);
        attachSla(ticket);
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    @Audited(action = "TICKET_STATUS_CHANGED", resource = "Ticket")
    public TicketResponse updateStatus(UUID ticketId, Ticket.TicketStatus newStatus) {
        Ticket ticket = resolveTicket(ticketId);
        validateTransition(ticket.getStatus(), newStatus);
        ticket.setStatus(newStatus);
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    @Audited(action = "TICKET_ASSIGNED", resource = "Ticket")
    public TicketResponse assign(UUID ticketId, UUID assigneeId) {
        Ticket ticket = resolveTicket(ticketId);
        ticket.assign(assigneeId);
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    @Audited(action = "TICKET_ESCALATED", resource = "Ticket")
    public TicketResponse escalate(UUID ticketId) {
        Ticket ticket = resolveTicket(ticketId);
        ticket.escalate();
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse addComment(UUID ticketId, AddCommentRequest request, UUID authorId) {
        Ticket ticket = resolveTicket(ticketId);
        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthorId(authorId);
        comment.setContent(request.getContent());
        comment.setInternal(request.isInternal());
        ticket.getComments().add(comment);
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    @Audited(action = "TICKET_CLOSED", resource = "Ticket")
    public void deleteTicket(UUID ticketId) {
        Ticket ticket = resolveTicket(ticketId);
        ticket.softDelete();
        ticketRepository.save(ticket);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private Ticket resolveTicket(UUID ticketId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return ticketRepository.findByIdAndTenantIdAndDeletedAtIsNull(ticketId, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        }
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
    }

    private void attachSla(Ticket ticket) {
        int hours = switch (ticket.getPriority()) {
            case LOW      -> SLA_LOW;
            case MEDIUM   -> SLA_MEDIUM;
            case HIGH     -> SLA_HIGH;
            case CRITICAL -> SLA_CRITICAL;
        };
        TicketSla sla = new TicketSla();
        sla.setTicket(ticket);
        sla.setDueAt(Instant.now().plus(Duration.ofHours(hours)));
        slaRepository.save(sla);
    }

    private void validateTransition(Ticket.TicketStatus from, Ticket.TicketStatus to) {
        boolean valid = switch (from) {
            case OPEN        -> to == Ticket.TicketStatus.IN_PROGRESS || to == Ticket.TicketStatus.RESOLVED || to == Ticket.TicketStatus.CLOSED;
            case IN_PROGRESS -> to == Ticket.TicketStatus.WAITING || to == Ticket.TicketStatus.RESOLVED || to == Ticket.TicketStatus.CLOSED;
            case WAITING     -> to == Ticket.TicketStatus.IN_PROGRESS || to == Ticket.TicketStatus.RESOLVED;
            case RESOLVED    -> to == Ticket.TicketStatus.CLOSED || to == Ticket.TicketStatus.OPEN;
            case CLOSED      -> to == Ticket.TicketStatus.OPEN;
        };
        if (!valid) {
            throw new ControlTowerException(
                "Invalid status transition: " + from + " → " + to, HttpStatus.BAD_REQUEST
            );
        }
    }

    private TicketResponse toResponse(Ticket t) {
        return TicketResponse.builder()
                .id(t.getId())
                .tenantId(t.getTenantId())
                .clientId(t.getClientId())
                .branchId(t.getBranchId())
                .title(t.getTitle())
                .description(t.getDescription())
                .priority(t.getPriority().name())
                .status(t.getStatus().name())
                .assigneeId(t.getAssigneeId())
                .source(t.getSource().name())
                .sourceRefId(t.getSourceRefId())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
