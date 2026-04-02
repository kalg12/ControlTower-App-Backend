package com.controltower.app.integrations.api;

import com.controltower.app.integrations.api.dto.IntegrationEndpointRequest;
import com.controltower.app.integrations.api.dto.PushEventRequest;
import com.controltower.app.integrations.application.IntegrationService;
import com.controltower.app.integrations.domain.IntegrationEndpoint;
import com.controltower.app.integrations.domain.IntegrationEvent;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Tag(name = "Integrations", description = "Generic integration endpoints for POS and third-party systems")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final IntegrationService integrationService;

    @Operation(summary = "List integration endpoints", description = "Returns a paginated list of all registered integration endpoints for the current tenant. Requires the 'integration:read' permission.")
    @GetMapping
    @PreAuthorize("hasAuthority('integration:read')")
    public ResponseEntity<ApiResponse<PageResponse<IntegrationEndpoint>>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                ApiResponse.ok(PageResponse.from(integrationService.listEndpoints(pageable))));
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

    @Operation(summary = "Delete integration endpoint", description = "Permanently removes an integration endpoint. Requires the 'integration:write' permission.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('integration:write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        integrationService.deactivate(id);
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
}
