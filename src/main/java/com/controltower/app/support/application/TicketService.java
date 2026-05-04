package com.controltower.app.support.application;

import com.controltower.app.clients.domain.ClientRepository;
import com.controltower.app.notifications.application.NotificationService;
import com.controltower.app.notifications.domain.Notification;
import com.controltower.app.shared.annotation.Audited;
import com.controltower.app.shared.exception.ControlTowerException;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.shared.infrastructure.EmailService;
import com.controltower.app.identity.domain.User;
import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.integrations.api.dto.PosTicketCommentDto;
import com.controltower.app.integrations.api.dto.PosTicketStatusResponse;
import com.controltower.app.notes.domain.NoteRepository;
import com.controltower.app.support.api.dto.*;
import com.controltower.app.support.domain.*;
import com.controltower.app.support.infrastructure.PosWebhookService;
import com.controltower.app.tenancy.domain.TenantContext;
import com.controltower.app.time.application.SlaConfigService;
import com.controltower.app.time.domain.TimeEntryRepository;
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
    private final PosWebhookService      posWebhookService;
    private final SlaConfigService       slaConfigService;
    private final UserRepository         userRepository;
    private final ClientRepository       clientRepository;
    private final NoteRepository         noteRepository;
    private final TimeEntryRepository    timeEntryRepository;
    private final NotificationService    notificationService;
    private final EmailService           emailService;

    @Transactional(readOnly = true)
    public Page<TicketResponse> listTickets(
            Ticket.TicketStatus status, Ticket.TicketSource source, UUID assigneeId, UUID clientId,
            Ticket.Priority priority, Instant createdAfter, Instant createdBefore,
            String q, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        Specification<Ticket> spec = buildFilterSpec(
                tenantId, status, source, assigneeId, clientId, priority, createdAfter, createdBefore, q);
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
                tenantId, status, null, assigneeId, clientId, priority, createdAfter, createdBefore, null);
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
        ticket.setEstimatedMinutes(request.getEstimatedMinutes());

        ticketRepository.save(ticket);
        attachSla(ticket);
        return toResponse(ticketRepository.save(ticket));
    }

    /** Returns all public (non-internal) comments for a ticket. */
    @Transactional(readOnly = true)
    public List<TicketCommentResponse> getPublicComments(UUID ticketId) {
        Ticket ticket = resolveTicket(ticketId);
        return ticket.getComments().stream()
                .filter(c -> !c.isInternal())
                .sorted(java.util.Comparator.comparing(TicketComment::getCreatedAt))
                .map(c -> {
                    String authorName = null;
                    if (c.getAuthorId() != null) {
                        authorName = userRepository.findById(c.getAuthorId())
                                .map(u -> u.getFullName())
                                .orElse(null);
                    }
                    return new TicketCommentResponse(
                            c.getId(),
                            c.getAuthorId(),
                            authorName,
                            c.getContent(),
                            c.isInternal(),
                            c.getAuthorId() != null ? "OPERATOR" : "POS_USER",
                            c.getCreatedAt()
                    );
                })
                .toList();
    }

    /** Returns aggregated stats for tickets from a given source (e.g. POS). */
    @Transactional(readOnly = true)
    public TicketStatsResponse getPosTicketStats() {
        UUID tenantId = TenantContext.getTenantId();
        long total = ticketRepository.countByTenantIdAndSourceAndDeletedAtIsNull(
                tenantId, Ticket.TicketSource.POS);
        List<Object[]> rows = ticketRepository.countByStatusForSource(tenantId, Ticket.TicketSource.POS);
        java.util.LinkedHashMap<String, Long> byStatus = new java.util.LinkedHashMap<>();
        for (Object[] row : rows) {
            byStatus.put(row[0].toString(), (Long) row[1]);
        }
        return new TicketStatsResponse(total, byStatus);
    }

    /** Returns a status summary for the CT ticket linked to a given POS ticket ID. */
    @Transactional(readOnly = true)
    public PosTicketStatusResponse getStatusForPosTicket(String posTicketId, UUID tenantId) {
        Ticket ticket = ticketRepository.findBySourceRefIdAndTenantIdIncludingDeleted(posTicketId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", posTicketId));
        // Soft-deleted ticket: tell POS it is closed so polling stops.
        if (ticket.getDeletedAt() != null) {
            return new PosTicketStatusResponse(
                    ticket.getId(), "CLOSED", ticket.getPriority().name(),
                    ticket.getAssigneeId(), null, 0);
        }
        List<TicketComment> publicComments = ticket.getComments().stream()
                .filter(c -> !c.isInternal())
                .toList();
        // firstCommentAt = first comment written by a CT operator (authorId != null).
        // POS_USER messages (authorId = null) do not count as an operator response.
        Instant firstCommentAt = publicComments.stream()
                .filter(c -> c.getAuthorId() != null)
                .map(TicketComment::getCreatedAt)
                .min(Instant::compareTo)
                .orElse(null);
        return new PosTicketStatusResponse(
                ticket.getId(),
                ticket.getStatus().name(),
                ticket.getPriority().name(),
                ticket.getAssigneeId(),
                firstCommentAt,
                publicComments.size()
        );
    }

    /** Returns public comments on the CT ticket linked to a given POS ticket ID, optionally filtered by `since`. */
    @Transactional(readOnly = true)
    public List<PosTicketCommentDto> getPublicCommentsSince(String posTicketId, UUID tenantId, Instant since) {
        Ticket ticket = ticketRepository.findBySourceRefIdAndTenantIdIncludingDeleted(posTicketId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", posTicketId));
        // Soft-deleted ticket: return empty — stops POS polling gracefully.
        if (ticket.getDeletedAt() != null) return java.util.List.of();
        return ticket.getComments().stream()
                .filter(c -> !c.isInternal())
                .filter(c -> since == null || c.getCreatedAt().isAfter(since))
                .sorted(java.util.Comparator.comparing(TicketComment::getCreatedAt))
                .map(c -> new PosTicketCommentDto(
                        c.getId(),
                        c.getContent(),
                        c.getAuthorId() != null ? "OPERATOR" : "POS_USER",
                        c.getCreatedAt()
                ))
                .toList();
    }

    /** Adds a public comment on the CT ticket linked to a POS ticket (authorId=null marks POS origin). */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void addExternalComment(String posTicketId, UUID tenantId, String content) {
        Ticket ticket = ticketRepository.findBySourceRefIdAndTenantIdIncludingDeleted(posTicketId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", posTicketId));
        // Skip silently if ticket was soft-deleted in CT.
        if (ticket.getDeletedAt() != null) return;
        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthorId(null);  // null = POS/external origin
        comment.setContent(content);
        comment.setInternal(false);
        ticket.getComments().add(comment);
        ticketRepository.save(ticket);
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
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
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
    public TicketResponse updateStatus(UUID ticketId, Ticket.TicketStatus newStatus, UUID changedByUserId) {
        Ticket ticket = resolveTicket(ticketId);
        validateTransition(ticket.getStatus(), newStatus);
        ticket.setStatus(newStatus);
        Ticket saved = ticketRepository.save(ticket);

        // Auto-stop any running time entries so SLA and logged time freeze at resolution
        if (newStatus == Ticket.TicketStatus.RESOLVED || newStatus == Ticket.TicketStatus.CLOSED) {
            timeEntryRepository.findActiveByTicket(ticketId).forEach(entry -> {
                entry.stop();
                timeEntryRepository.save(entry);
            });
        }

        // Notify POS Backend so its ctStatus updates immediately (no wait for cron)
        if (saved.getSource() == Ticket.TicketSource.POS && saved.getSourceRefId() != null) {
            String callbackUrl = saved.getPosContext() != null
                    ? (String) saved.getPosContext().get("callbackUrl") : null;
            posWebhookService.notifyStatusChange(saved.getSourceRefId(), callbackUrl, newStatus.name());
        }
        // Notify all relevant recipients (assignee + commenters + all agents), excluding the user who changed the status
        notifyTicketChange(
                saved,
                changedByUserId,
                "TICKET_STATUS_CHANGED",
                "Estado de ticket actualizado",
                "El ticket \"" + saved.getTitle() + "\" cambió a " + newStatus.name(),
                Map.of("ticketId", saved.getId().toString(), "newStatus", newStatus.name()));
        return toResponse(saved);
    }

    @Transactional
    @Audited(action = "TICKET_ASSIGNED", resource = "Ticket")
    public TicketResponse assign(UUID ticketId, UUID assigneeId, UUID assignedByUserId) {
        Ticket ticket = resolveTicket(ticketId);
        ticket.assign(assigneeId);
        Ticket saved = ticketRepository.save(ticket);
        notifyTicketChange(
                saved,
                assignedByUserId,
                "TICKET_ASSIGNED",
                "Ticket asignado",
                "Se te asignó el ticket: " + saved.getTitle(),
                Map.of("ticketId", saved.getId().toString()));
        return toResponse(saved);
    }

    /**
     * Auto-assigns the ticket to the active user in the current tenant
     * who currently has the fewest open (non-resolved/closed) tickets.
     * Falls back to the first available user if nobody has any tickets yet.
     */
    @Transactional
    @Audited(action = "TICKET_AUTO_ASSIGNED", resource = "Ticket")
    public TicketResponse autoAssign(UUID ticketId) {
        UUID tenantId = TenantContext.getTenantId();

        // Build workload map: userId → openTicketCount
        Map<UUID, Long> workload = new java.util.HashMap<>();
        ticketRepository.countOpenTicketsPerAssignee(tenantId)
                .forEach(row -> workload.put((UUID) row[0], (Long) row[1]));

        // Find the tenant user with the smallest open-ticket count
        UUID assigneeId = userRepository
                .findByTenantIdAndDeletedAtIsNull(tenantId, org.springframework.data.domain.PageRequest.of(0, 200))
                .stream()
                .filter(u -> "ACTIVE".equals(u.getStatus()))
                .min(java.util.Comparator.comparingLong(u -> workload.getOrDefault(u.getId(), 0L)))
                .map(u -> u.getId())
                .orElseThrow(() -> new ControlTowerException("No active users found in tenant", org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY));

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
        Ticket saved = ticketRepository.save(ticket);
        // Notify POS Backend when an operator posts a public comment on a POS ticket
        if (saved.getSource() == Ticket.TicketSource.POS &&
                saved.getSourceRefId() != null &&
                !request.isInternal()) {
            String callbackUrl = saved.getPosContext() != null
                    ? (String) saved.getPosContext().get("callbackUrl") : null;
            posWebhookService.notifyNewComment(saved.getSourceRefId(), callbackUrl, request.getContent(), authorId.toString());
        }
        // Notify all relevant recipients about the new comment (excluding the author)
        notifyTicketChange(
                saved,
                authorId,
                "TICKET_NEW_COMMENT",
                "Nuevo comentario en ticket",
                "Nuevo comentario en: " + saved.getTitle(),
                Map.of("ticketId", saved.getId().toString()));

        // Send email notifications for public comments
        if (!request.isInternal()) {
            String agentName = userRepository.findById(authorId)
                    .map(User::getFullName).orElse("Operador CT");

            // Manual ticket with linked client
            if (saved.getClientId() != null) {
                clientRepository.findById(saved.getClientId()).ifPresent(client -> {
                    if (client.getPrimaryEmail() != null && !client.getPrimaryEmail().isBlank()) {
                        emailService.sendTicketCommentNotification(
                                client.getPrimaryEmail(), saved.getTitle(),
                                agentName, request.getContent());
                    }
                });
            }

            // POS ticket: notify submitter and manager if emails are in posContext
            if (saved.getSource() == Ticket.TicketSource.POS && saved.getPosContext() != null) {
                String submitterEmail = (String) saved.getPosContext().get("submitterEmail");
                String managerEmail   = (String) saved.getPosContext().get("managerEmail");
                for (String email : new String[]{submitterEmail, managerEmail}) {
                    if (email != null && !email.isBlank()) {
                        emailService.sendTicketCommentNotification(
                                email, saved.getTitle(), agentName, request.getContent());
                    }
                }
            }
        }

        return toResponse(saved);
    }

    @Transactional
    @Audited(action = "TICKET_CLOSED", resource = "Ticket")
    public void deleteTicket(UUID ticketId) {
        Ticket ticket = resolveTicket(ticketId);
        ticket.softDelete();
        ticketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> listDeleted(Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        return ticketRepository.findDeletedByTenantId(tenantId, pageable).map(this::toResponse);
    }

    @Transactional
    @Audited(action = "TICKET_RESTORED", resource = "Ticket")
    public TicketResponse restoreTicket(UUID ticketId) {
        UUID tenantId = TenantContext.getTenantId();
        Ticket ticket = ticketRepository.findDeletedById(ticketId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        ticket.setDeletedAt(null);
        ticket.setStatus(Ticket.TicketStatus.OPEN);
        return toResponse(ticketRepository.save(ticket));
    }

    /**
     * Merges {@code sourceId} into {@code targetId}.
     * Moves all comments, notes, and time entries from source → target, then soft-deletes source.
     */
    @Transactional
    public TicketResponse mergeTicket(UUID sourceId, UUID targetId) {
        UUID tenantId = TenantContext.getTenantId();
        if (sourceId.equals(targetId)) {
            throw new ControlTowerException("Cannot merge a ticket into itself", HttpStatus.BAD_REQUEST);
        }
        Ticket source = resolveTicket(sourceId);
        Ticket target = resolveTicket(targetId);

        // Move comments
        source.getComments().forEach(c -> c.setTicket(target));

        // Move notes (linkedId reassignment)
        org.springframework.data.domain.PageRequest allNotes =
            org.springframework.data.domain.PageRequest.of(0, 1000);
        noteRepository.findByTenantIdAndLinkedToAndLinkedIdAndDeletedAtIsNull(
                tenantId, "TICKET", sourceId, allNotes)
            .forEach(n -> {
                n.setLinkedId(targetId);
                noteRepository.save(n);
            });

        // Move time entries
        timeEntryRepository.findByEntityTypeAndEntityId(
                com.controltower.app.time.domain.TimeEntry.EntityType.TICKET, sourceId)
            .forEach(e -> {
                e.setEntityId(targetId);
                timeEntryRepository.save(e);
            });

        source.softDelete();
        ticketRepository.save(source);
        return toResponse(ticketRepository.save(target));
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private Specification<Ticket> buildFilterSpec(
            UUID tenantId,
            Ticket.TicketStatus status,
            Ticket.TicketSource source,
            UUID assigneeId,
            UUID clientId,
            Ticket.Priority priority,
            Instant createdAfter,
            Instant createdBefore,
            String q) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (source != null) {
                predicates.add(cb.equal(root.get("source"), source));
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
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")),       pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
                ));
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
        int hours = slaConfigService.getWindowHours(ticket.getPriority(), ticket.getTenantId());
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
        TicketSla sla = t.getSla();
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
                .estimatedMinutes(t.getEstimatedMinutes())
                .slaDueAt(sla != null ? sla.getDueAt() : null)
                .slaBreached(sla != null ? sla.isBreached() : null)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .deletedAt(t.getDeletedAt())
                .build();
    }

    /**
     * Returns all users who should receive notifications about a ticket (assignee + commenters + all agents).
     * The authorUserId (the user who triggered the notification) is excluded to avoid self-notification.
     */
    private List<UUID> getNotificationRecipients(Ticket ticket, UUID authorUserId) {
        java.util.Set<UUID> recipients = new java.util.HashSet<>();

        // 1. Add assignee (if exists and not the author)
        if (ticket.getAssigneeId() != null && !ticket.getAssigneeId().equals(authorUserId)) {
            recipients.add(ticket.getAssigneeId());
        }

        // 2. Add all unique commenters (authors of public comments, excluding internal)
        ticket.getComments().stream()
                .filter(c -> !c.isInternal() && c.getAuthorId() != null && !c.getAuthorId().equals(authorUserId))
                .map(TicketComment::getAuthorId)
                .forEach(recipients::add);

        // 3. Add all agents in the tenant (ticket:read permission)
        List<User> agents = userRepository.findByTenantIdAndPermission(
                ticket.getTenantId(), "ticket:read");
        agents.stream()
                .map(u -> u.getId())
                .filter(uid -> !uid.equals(authorUserId))
                .forEach(recipients::add);

        return new java.util.ArrayList<>(recipients);
    }

    /**
     * Sends a notification to all relevant recipients about a ticket change.
     */
    private void notifyTicketChange(Ticket ticket, UUID authorUserId, String type, String title, String body,
                                     Map<String, Object> metadata) {
        List<UUID> recipients = getNotificationRecipients(ticket, authorUserId);
        if (!recipients.isEmpty()) {
            notificationService.send(
                    ticket.getTenantId(),
                    type,
                    title,
                    body,
                    Notification.Severity.INFO,
                    metadata,
                    recipients);
        }
    }
}
