package com.controltower.app.clients.api;

import com.controltower.app.clients.api.dto.*;
import com.controltower.app.clients.application.ClientInteractionService;
import com.controltower.app.clients.application.ClientOpportunityService;
import com.controltower.app.clients.application.ClientService;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import com.controltower.app.tenancy.domain.TenantContext;
import com.controltower.app.users.domain.User;
import com.controltower.app.users.domain.UserRepository;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/**
 * CRM endpoints for Client Interactions (activity log) and Opportunities (pipeline).
 */
@Tag(name = "CRM", description = "Client CRM — interactions, opportunities, pipeline")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientCrmController {

    private final ClientInteractionService interactionService;
    private final ClientOpportunityService opportunityService;
    private final ClientService            clientService;
    private final UserRepository           userRepository;

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", auth.getName()));
    }

    // ── Client Interactions ───────────────────────────────────────────

    @Operation(summary = "List client interactions", description = "Paginated activity log for a client. Requires 'client:read'.")
    @GetMapping("/{clientId}/interactions")
    @PreAuthorize("hasAuthority('client:read')")
    public ResponseEntity<ApiResponse<PageResponse<ClientInteractionResponse>>> listInteractions(
            @PathVariable UUID clientId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("occurredAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(interactionService.listByClient(clientId, pageable))));
    }

    @Operation(summary = "Log interaction", description = "Record a call, meeting, email, or other activity for a client. Requires 'client:write'.")
    @PostMapping("/{clientId}/interactions")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<ClientInteractionResponse>> logInteraction(
            @PathVariable UUID clientId,
            @Valid @RequestBody ClientInteractionRequest request,
            Authentication auth) {
        User user = getCurrentUser(auth);
        ClientInteractionResponse created = interactionService.create(clientId, request, user.getId(), user.getFullName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Interaction logged", created));
    }

    @Operation(summary = "Delete interaction", description = "Remove an interaction from the activity log. Requires 'client:write'.")
    @DeleteMapping("/interactions/{interactionId}")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<Void>> deleteInteraction(@PathVariable UUID interactionId) {
        interactionService.delete(interactionId);
        return ResponseEntity.ok(ApiResponse.ok("Interaction removed"));
    }

    // ── Client Opportunities (Pipeline) ──────────────────────────────

    @Operation(summary = "List client opportunities", description = "Paginated list of sales opportunities for a client. Requires 'client:read'.")
    @GetMapping("/{clientId}/opportunities")
    @PreAuthorize("hasAuthority('client:read')")
    public ResponseEntity<ApiResponse<PageResponse<ClientOpportunityResponse>>> listOpportunities(
            @PathVariable UUID clientId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(opportunityService.listByClient(clientId, pageable))));
    }

    @Operation(summary = "List all opportunities (pipeline)", description = "Paginated list of all sales opportunities in the tenant's pipeline. Requires 'client:read'.")
    @GetMapping("/opportunities")
    @PreAuthorize("hasAuthority('client:read')")
    public ResponseEntity<ApiResponse<PageResponse<ClientOpportunityResponse>>> listAllOpportunities(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(opportunityService.listAll(pageable))));
    }

    @Operation(summary = "Get active pipeline", description = "Returns all open opportunities (not closed won/lost), ordered by value. Requires 'client:read'.")
    @GetMapping("/opportunities/pipeline")
    @PreAuthorize("hasAuthority('client:read')")
    public ResponseEntity<ApiResponse<List<ClientOpportunityResponse>>> getActivePipeline() {
        return ResponseEntity.ok(ApiResponse.ok(opportunityService.getActivePipeline()));
    }

    @Operation(summary = "Create opportunity", description = "Add a new sales opportunity for a client. Requires 'client:write'.")
    @PostMapping("/{clientId}/opportunities")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<ClientOpportunityResponse>> createOpportunity(
            @PathVariable UUID clientId,
            @Valid @RequestBody ClientOpportunityRequest request) {
        ClientOpportunityResponse created = opportunityService.create(clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Opportunity created", created));
    }

    @Operation(summary = "Update opportunity", description = "Update an opportunity's stage, value, owner, etc. Requires 'client:write'.")
    @PutMapping("/opportunities/{oppId}")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<ClientOpportunityResponse>> updateOpportunity(
            @PathVariable UUID oppId,
            @Valid @RequestBody ClientOpportunityRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Opportunity updated", opportunityService.update(oppId, request)));
    }

    @Operation(summary = "Delete opportunity", description = "Remove a sales opportunity. Requires 'client:write'.")
    @DeleteMapping("/opportunities/{oppId}")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<Void>> deleteOpportunity(@PathVariable UUID oppId) {
        opportunityService.delete(oppId);
        return ResponseEntity.ok(ApiResponse.ok("Opportunity removed"));
    }
}
