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
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Tag(name = "Integrations", description = "Generic integration endpoints for POS and third-party systems")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final IntegrationService integrationService;

    @GetMapping
    @PreAuthorize("hasAuthority('integration:read')")
    public ResponseEntity<ApiResponse<PageResponse<IntegrationEndpoint>>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                ApiResponse.ok(PageResponse.from(integrationService.listEndpoints(pageable))));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('integration:write')")
    public ResponseEntity<ApiResponse<IntegrationEndpoint>> register(
            @Valid @RequestBody IntegrationEndpointRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(integrationService.register(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('integration:write')")
    public ResponseEntity<ApiResponse<IntegrationEndpoint>> update(
            @PathVariable UUID id,
            @Valid @RequestBody IntegrationEndpointRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(integrationService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('integration:write')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        integrationService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok("Integration endpoint deactivated"));
    }

    /**
     * Receives a push event from an external client system.
     * Public endpoint — authenticated via X-Api-Key header.
     */
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
