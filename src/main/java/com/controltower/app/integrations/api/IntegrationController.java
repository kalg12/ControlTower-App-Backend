package com.controltower.app.integrations.api;

import com.controltower.app.integrations.api.dto.IntegrationEndpointRequest;
import com.controltower.app.integrations.api.dto.PosTicketCommentDto;
import com.controltower.app.integrations.api.dto.PosTicketStatusResponse;
import com.controltower.app.integrations.api.dto.PushEventRequest;
import com.controltower.app.integrations.application.IntegrationService;
import com.controltower.app.integrations.domain.IntegrationEndpoint;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Tag(name = "Integrations", description = "Generic integration endpoints for POS and third-party systems")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final IntegrationService integrationService;

    @Operation(summary = "List integration endpoints", description = "Returns a paginated list of all registered integration endpoints for the current tenant. Optionally filter by type (POS, CUSTOM). Requires the 'integration:read' permission.")
    @GetMapping
    @PreAuthorize("hasAuthority('integration:read')")
    public ResponseEntity<ApiResponse<PageResponse<IntegrationEndpoint>>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String type) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                ApiResponse.ok(PageResponse.from(integrationService.listEndpoints(pageable, type))));
    }

    @Operation(summary = "Register integration endpoint", description = "Registers a new external integration endpoint and generates an API key. Requires the 'integration:write' permission.")
    @PostMapping
    @PreAuthorize("hasAuthority('integration:write')")
    public ResponseEntity<ApiResponse<IntegrationEndpoint>> register(
            @Valid @RequestBody IntegrationEndpointRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(integrationService.register(request)));
    }

    @Operation(summary = "Update integration endpoint", description = "Updates the configuration of an existing integration endpoint. Requires the 'integration:write' permission.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('integration:write')")
    public ResponseEntity<ApiResponse<IntegrationEndpoint>> update(
            @PathVariable UUID id,
            @Valid @RequestBody IntegrationEndpointRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(integrationService.update(id, request)));
    }

    @Operation(summary = "Activate integration endpoint", description = "Re-activates a previously deactivated integration endpoint. Requires the 'integration:write' permission.")
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('integration:write')")
    public ResponseEntity<ApiResponse<IntegrationEndpoint>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(integrationService.activate(id)));
    }

    @Operation(summary = "Deactivate integration endpoint", description = "Deactivates the specified integration endpoint, preventing further event ingestion via its API key. Requires the 'integration:write' permission.")
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('integration:write')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        integrationService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok("Integration endpoint deactivated"));
    }

    @Operation(summary = "Delete integration endpoint", description = "Soft-deletes an integration endpoint — it will no longer appear in listings or be polled. Requires the 'integration:write' permission.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('integration:write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        integrationService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Integration endpoint removed"));
    }

    /**
     * Receives a push event from an external client system.
     * Public endpoint — authenticated via X-Api-Key header.
     */
    @Operation(summary = "Ingest integration event", description = "Receives a push event from an external system. Public endpoint authenticated via the X-Api-Key header tied to a registered integration endpoint.")
    @PostMapping("/events")
    public ResponseEntity<ApiResponse<Void>> ingestEvent(
            @Valid @RequestBody PushEventRequest request,
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey) {
        integrationService.ingestEvent(
                request.getEndpointId(),
                apiKey,
                request.getEventType(),
                request.getPayload()
        );
        return ResponseEntity.ok(ApiResponse.ok("Event received"));
    }

    /**
     * Returns the CT status of a POS ticket. Public endpoint — authenticated via X-Api-Key header.
     */
    @Operation(summary = "Get POS ticket status", description = "Returns the current CT status for a ticket submitted from POS. Authenticated via X-Api-Key.")
    @GetMapping("/{endpointId}/pos-tickets/{posTicketId}/status")
    public ResponseEntity<ApiResponse<PosTicketStatusResponse>> getPosTicketStatus(
            @PathVariable UUID endpointId,
            @PathVariable String posTicketId,
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey) {
        return ResponseEntity.ok(ApiResponse.ok(
                integrationService.getPosTicketStatus(endpointId, apiKey, posTicketId)));
    }

    /**
     * Returns public comments on a POS ticket (for chat sync). Public endpoint — authenticated via X-Api-Key header.
     */
    @Operation(summary = "Get POS ticket comments", description = "Returns public comments on the CT ticket linked to a POS ticket ID. Used by POS Backend to sync chat replies. Authenticated via X-Api-Key.")
    @GetMapping("/{endpointId}/pos-tickets/{posTicketId}/comments")
    public ResponseEntity<ApiResponse<List<PosTicketCommentDto>>> getPosTicketComments(
            @PathVariable UUID endpointId,
            @PathVariable String posTicketId,
            @Parameter(description = "Return only comments created after this timestamp (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since,
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey) {
        return ResponseEntity.ok(ApiResponse.ok(
                integrationService.getPosTicketComments(endpointId, apiKey, posTicketId, since)));
    }
}
