package com.controltower.app.support.application;

import com.controltower.app.shared.annotation.Audited;
import com.controltower.app.shared.exception.ControlTowerException;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.support.api.dto.*;
import com.controltower.app.support.domain.*;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository       ticketRepository;
    private final TicketSlaRepository    slaRepository;
    private final ApplicationEventPublisher publisher;

    // SLA windows by priority (hours)
    private static final int SLA_LOW      = 48;
    private static final int SLA_MEDIUM   = 24;
    private static final int SLA_HIGH     = 8;
    private static final int SLA_CRITICAL = 2;

    @Transactional(readOnly = true)
    public Page<TicketResponse> listTickets(
            Ticket.TicketStatus status, UUID assigneeId, UUID clientId,
            Ticket.Priority priority, Instant createdAfter, Instant createdBefore,
            Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        Specification<Ticket> spec = buildFilterSpec(
                tenantId, status, assigneeId, clientId, priority, createdAfter, createdBefore);
        return ticketRepository.findAll(spec, pageable)
                .map(this::toResponse);
    }

    /** Returns tickets whose SLA window expires within the next {@code withinHours} hours
     *  and have not been breached yet (i.e., still actionable). */
    @Transactional(readOnly = true)
    public Page<TicketResponse> listSlaAtRisk(int withinHours, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        Instant threshold = Instant.now().plus(Duration.ofHours(withinHours));
        return ticketRepository.findSlaAtRisk(tenantId, threshold, pageable)
                .map(this::toResponse);
    }

    /** Bulk status update — applies state-machine validation to each ticket. */
    @Transactional
    @Audited(action = "TICKET_BULK_STATUS", resource = "Ticket")
    public List<TicketResponse> bulkUpdateStatus(List<UUID> ticketIds, Ticket.TicketStatus newStatus) {
        UUID tenantId = TenantContext.getTenantId();
        List<Ticket> tickets = ticketRepository.findByIdInAndTenantIdAndDeletedAtIsNull(ticketIds, tenantId);
        tickets.forEach(t -> {
            validateTransition(t.getStatus(), newStatus);
            t.setStatus(newStatus);
        });
        return ticketRepository.saveAll(tickets).stream().map(this::toResponse).toList();
    }

    /** Bulk assign — assigns all given tickets to the specified user. */
    @Transactional
    @Audited(action = "TICKET_BULK_ASSIGN", resource = "Ticket")
    public List<TicketResponse> bulkAssign(List<UUID> ticketIds, UUID assigneeId) {
        UUID tenantId = TenantContext.getTenantId();
        List<Ticket> tickets = ticketRepository.findByIdInAndTenantIdAndDeletedAtIsNull(ticketIds, tenantId);
        tickets.forEach(t -> t.assign(assigneeId));
        return ticketRepository.saveAll(tickets).stream().map(this::toResponse).toList();
    }

    /** Writes a CSV of all tickets matching the filters to the given writer. */
    @Transactional(readOnly = true)
    public void exportCsv(PrintWriter writer,
                          Ticket.TicketStatus status, UUID assigneeId, UUID clientId,
                          Ticket.Priority priority, Instant createdAfter, Instant createdBefore) {
        UUID tenantId = TenantContext.getTenantId();
        Specification<Ticket> spec = buildFilterSpec(
                tenantId, status, assigneeId, clientId, priority, createdAfter, createdBefore);
        List<Ticket> tickets = ticketRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));

        writer.println("id,title,status,priority,assigneeId,clientId,branchId,source,createdAt,updatedAt");
        for (Ticket t : tickets) {
            writer.printf("%s,\"%s\",%s,%s,%s,%s,%s,%s,%s,%s%n",
                    t.getId(),
                    escapeCsv(t.getTitle()),
                    t.getStatus(),
                    t.getPriority(),
                    orEmpty(t.getAssigneeId()),
                    orEmpty(t.getClientId()),
                    orEmpty(t.getBranchId()),
                    t.getSource(),
                    t.getCreatedAt(),
                    t.getUpdatedAt());
        }
        writer.flush();
    }

    private static String escapeCsv(String value) {
        return value == null ? "" : value.replace("\"", "\"\"");
    }

    private static String orEmpty(Object o) {
        return o == null ? "" : o.toString();
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

    /** Called internally (no TenantContext) when a POS system submits a support ticket. */
    @Transactional
    public TicketResponse createFromPosEvent(
            UUID tenantId, String posTicketId, String title, String description,
            Ticket.Priority priority, UUID branchId, Map<String, Object> posContext) {

        Ticket ticket = new Ticket();
        ticket.setTenantId(tenantId);
        ticket.setBranchId(branchId);
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setPriority(priority);
        ticket.setSource(Ticket.TicketSource.POS);
        ticket.setSourceRefId(posTicketId);
        ticket.setPosContext(posContext);
        ticketRepository.save(ticket);
        attachSla(ticket);
        ticketRepository.save(ticket);

        String branchName  = posContext != null ? (String) posContext.getOrDefault("branchName",  "") : "";
        String submittedBy = posContext != null ? (String) posContext.getOrDefault("submittedBy", "") : "";
        publisher.publishEvent(new PosTicketReceivedEvent(ticket, branchName, submittedBy));

        return toResponse(ticket);
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

    private Specification<Ticket> buildFilterSpec(
            UUID tenantId,
            Ticket.TicketStatus status,
            UUID assigneeId,
            UUID clientId,
            Ticket.Priority priority,
            Instant createdAfter,
            Instant createdBefore) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (assigneeId != null) {
                predicates.add(cb.equal(root.get("assigneeId"), assigneeId));
            }
            if (clientId != null) {
                predicates.add(cb.equal(root.get("clientId"), clientId));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (createdAfter != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdAfter));
            }
            if (createdBefore != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdBefore));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

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
        List<String> labels = t.getLabels() != null ? java.util.Arrays.asList(t.getLabels()) : java.util.List.of();
        int commentsCount = t.getComments() != null ? t.getComments().size() : 0;
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
                .posContext(t.getPosContext())
                .labels(labels)
                .commentsCount(commentsCount)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
