package com.controltower.app.clients.api;

import com.controltower.app.clients.api.dto.*;
import com.controltower.app.clients.application.ClientService;
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

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/**
 * Client and Branch management endpoints.
 * All data is automatically scoped to the authenticated user's tenant
 * via TenantContext (set by TenantInterceptor).
 */
@Tag(name = "Clients", description = "Client and branch management")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    // ── Clients ───────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('client:read')")
    public ResponseEntity<ApiResponse<PageResponse<ClientResponse>>> listClients(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(clientService.listClients(search, pageable))));
    }

    @GetMapping("/{clientId}")
    @PreAuthorize("hasAuthority('client:read')")
    public ResponseEntity<ApiResponse<ClientResponse>> getClient(@PathVariable UUID clientId) {
        return ResponseEntity.ok(ApiResponse.ok(clientService.getClient(clientId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<ClientResponse>> createClient(
            @Valid @RequestBody ClientRequest request) {
        ClientResponse created = clientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Client created", created));
    }

    @PutMapping("/{clientId}")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<ClientResponse>> updateClient(
            @PathVariable UUID clientId,
            @Valid @RequestBody ClientRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Client updated", clientService.updateClient(clientId, request)));
    }

    @DeleteMapping("/{clientId}")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<Void>> deleteClient(@PathVariable UUID clientId) {
        clientService.deleteClient(clientId);
        return ResponseEntity.ok(ApiResponse.ok("Client deleted"));
    }

    // ── Branches ──────────────────────────────────────────────────────

    @GetMapping("/{clientId}/branches")
    @PreAuthorize("hasAuthority('client:read')")
    public ResponseEntity<ApiResponse<List<BranchResponse>>> listBranches(@PathVariable UUID clientId) {
        return ResponseEntity.ok(ApiResponse.ok(clientService.listBranches(clientId)));
    }

    @PostMapping("/{clientId}/branches")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<BranchResponse>> createBranch(
            @PathVariable UUID clientId,
            @Valid @RequestBody BranchRequest request) {
        BranchResponse created = clientService.createBranch(clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Branch created", created));
    }

    @DeleteMapping("/branches/{branchId}")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable UUID branchId) {
        clientService.deleteBranch(branchId);
        return ResponseEntity.ok(ApiResponse.ok("Branch deleted"));
    }
}
