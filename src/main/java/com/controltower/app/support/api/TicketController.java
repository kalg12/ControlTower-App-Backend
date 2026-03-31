package com.controltower.app.support.api;

import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import com.controltower.app.support.api.dto.AddCommentRequest;
import com.controltower.app.support.api.dto.CreateTicketRequest;
import com.controltower.app.support.api.dto.TicketResponse;
import com.controltower.app.support.application.TicketService;
import com.controltower.app.support.domain.Ticket;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    @PreAuthorize("hasAuthority('ticket:read')")
    public ResponseEntity<ApiResponse<PageResponse<TicketResponse>>> list(
            @RequestParam(required = false) Ticket.TicketStatus status,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                ApiResponse.ok(PageResponse.from(ticketService.listTickets(status, assigneeId, clientId, pageable))));
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
    public ResponseEntity<ApiResponse<TicketResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestParam Ticket.TicketStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.updateStatus(id, status)));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('ticket:write')")
    public ResponseEntity<ApiResponse<TicketResponse>> assign(
            @PathVariable UUID id,
            @RequestParam UUID assigneeId) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.assign(id, assigneeId)));
    }

    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasAuthority('ticket:write')")
    public ResponseEntity<ApiResponse<TicketResponse>> escalate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.escalate(id)));
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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ticket:write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.ok(ApiResponse.ok("Ticket closed"));
    }
}
