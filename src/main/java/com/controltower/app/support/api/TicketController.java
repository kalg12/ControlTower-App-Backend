package com.controltower.app.support.api;

import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import com.controltower.app.shared.exception.ControlTowerException;
import com.controltower.app.support.api.dto.*;
import com.controltower.app.support.application.TicketService;
import com.controltower.app.support.domain.Ticket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Tag(name = "Tickets", description = "Support ticket management with SLA tracking")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    @PreAuthorize("hasAuthority('ticket:read')")
    @Operation(
        summary = "List tickets",
        description = "Returns a paginated list of tickets with optional filters. " +
                      "Use `slaAtRisk=true` to see tickets whose SLA expires within the next `slaWindowHours` hours."
    )
    public ResponseEntity<ApiResponse<PageResponse<TicketResponse>>> list(
            @RequestParam(required = false) Ticket.TicketStatus status,
            @RequestParam(required = false) Ticket.TicketSource source,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) Ticket.Priority priority,
            @Parameter(description = "ISO-8601 instant, e.g. 2026-01-01T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdAfter,
            @Parameter(description = "ISO-8601 instant, e.g. 2026-12-31T23:59:59Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdBefore,
            @Parameter(description = "Return only tickets at SLA risk (overrides other filters)")
            @RequestParam(defaultValue = "false") boolean slaAtRisk,
            @Parameter(description = "Used with slaAtRisk=true: hours until SLA expiry (default 4)")
            @RequestParam(defaultValue = "4") int slaWindowHours,
            @Parameter(description = "Full-text search on title and description")
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (slaAtRisk) {
            return ResponseEntity.ok(ApiResponse.ok(
                    PageResponse.from(ticketService.listSlaAtRisk(slaWindowHours, pageable))));
        }

        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(
                ticketService.listTickets(status, source, assigneeId, clientId, priority, createdAfter, createdBefore, q, pageable))));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('ticket:read')")
    @Operation(summary = "POS ticket stats", description = "Returns aggregated counts per status for tickets from the POS source.")
    public ResponseEntity<ApiResponse<TicketStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.getPosTicketStats()));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('ticket:read')")
    @Operation(
        summary = "Export tickets as CSV",
        description = "Streams a CSV file of all tickets matching the given filters. " +
                      "Same filter parameters as GET /tickets."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "CSV stream (attachment)",
        content = @Content(mediaType = "text/csv")
    )
    public void exportCsv(
            @RequestParam(required = false) Ticket.TicketStatus status,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) Ticket.Priority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdBefore,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"tickets.csv\"");
        ticketService.exportCsv(response.getWriter(),
                status, assigneeId, clientId, priority, createdAfter, createdBefore);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ticket:write')")
    public ResponseEntity<ApiResponse<TicketResponse>> create(
            @Valid @RequestBody CreateTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(ticketService.createTicket(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ticket:read')")
    public ResponseEntity<ApiResponse<TicketResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.getTicket(id)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ticket:write')")
    @Operation(summary = "Update ticket status", description = "Accepts `status` either as query parameter or JSON body. Applies ticket state-machine rules.")
    public ResponseEntity<ApiResponse<TicketResponse>> updateStatus(
            @PathVariable UUID id,
            @Parameter(description = "Target status", required = false)
            @RequestParam(required = false) Ticket.TicketStatus status,
            @RequestBody(required = false) UpdateTicketStatusRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Ticket.TicketStatus targetStatus = status != null ? status : request != null ? request.getStatus() : null;
        if (targetStatus == null) {
            throw new ControlTowerException("status is required", HttpStatus.BAD_REQUEST);
        }
        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(ticketService.updateStatus(id, targetStatus, userId)));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('ticket:write')")
    public ResponseEntity<ApiResponse<TicketResponse>> assign(
            @PathVariable UUID id,
            @RequestParam UUID assigneeId,
            @AuthenticationPrincipal UserDetails principal) {
        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(ticketService.assign(id, assigneeId, userId)));
    }

    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasAuthority('ticket:write')")
    public ResponseEntity<ApiResponse<TicketResponse>> escalate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.escalate(id)));
    }

    @GetMapping("/{id}/comments")
    @PreAuthorize("hasAuthority('ticket:read')")
    @Operation(summary = "List public comments", description = "Returns all non-internal comments for a ticket, ordered by creation time.")
    public ResponseEntity<ApiResponse<List<TicketCommentResponse>>> getComments(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.getPublicComments(id)));
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAuthority('ticket:write')")
    public ResponseEntity<ApiResponse<TicketResponse>> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody AddCommentRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        UUID authorId = UUID.fromString(principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(ticketService.addComment(id, request, authorId)));
    }

    @PostMapping("/bulk/status")
    @PreAuthorize("hasAuthority('ticket:write')")
    @Operation(
        summary = "Bulk status update",
        description = "Updates the status of multiple tickets in one request. " +
                      "State-machine rules are applied to each ticket; if any transition is invalid the whole batch fails."
    )
    public ResponseEntity<ApiResponse<List<TicketResponse>>> bulkStatus(
            @Valid @RequestBody BulkStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                ticketService.bulkUpdateStatus(request.getTicketIds(), request.getStatus())));
    }

    @PostMapping("/bulk/assign")
    @PreAuthorize("hasAuthority('ticket:write')")
    @Operation(
        summary = "Bulk assign",
        description = "Assigns multiple tickets to a single user. " +
                      "Tickets already in OPEN status are automatically transitioned to IN_PROGRESS."
    )
    public ResponseEntity<ApiResponse<List<TicketResponse>>> bulkAssign(
            @Valid @RequestBody BulkAssignRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                ticketService.bulkAssign(request.getTicketIds(), request.getAssigneeId())));
    }

    @Operation(
        summary = "Auto-assign ticket",
        description = "Assigns the ticket to the active tenant user who currently has the fewest open tickets."
    )
    @PostMapping("/{id}/auto-assign")
    @PreAuthorize("hasAuthority('ticket:write')")
    public ResponseEntity<ApiResponse<TicketResponse>> autoAssign(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Ticket auto-assigned", ticketService.autoAssign(id)));
    }

    @PostMapping("/{id}/merge")
    @PreAuthorize("hasAuthority('ticket:write')")
    @Operation(summary = "Merge ticket into another",
               description = "Moves all comments, notes, and time entries from this ticket into the target, then soft-deletes this ticket.")
    public ResponseEntity<ApiResponse<TicketResponse>> merge(
            @PathVariable UUID id,
            @RequestParam UUID targetId) {
        return ResponseEntity.ok(ApiResponse.ok("Ticket merged", ticketService.mergeTicket(id, targetId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ticket:write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.ok(ApiResponse.ok("Ticket deleted"));
    }

    @Operation(summary = "List deleted tickets (trash)", description = "Returns soft-deleted tickets for the current tenant. Requires 'ticket:read'.")
    @GetMapping("/trash")
    @PreAuthorize("hasAuthority('ticket:read')")
    public ResponseEntity<ApiResponse<PageResponse<TicketResponse>>> listTrash(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("deletedAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(ticketService.listDeleted(pageable))));
    }

    @Operation(summary = "Restore ticket from trash", description = "Restores a soft-deleted ticket back to OPEN status. Requires 'ticket:write'.")
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('ticket:write')")
    public ResponseEntity<ApiResponse<TicketResponse>> restore(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Ticket restored", ticketService.restoreTicket(id)));
    }
}
