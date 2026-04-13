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
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "List clients", description = "Returns a paginated list of clients scoped to the current tenant. Supports optional full-text search. Requires the 'client:read' permission.")
    @GetMapping
    @PreAuthorize("hasAuthority('client:read')")
    public ResponseEntity<ApiResponse<PageResponse<ClientResponse>>> listClients(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(clientService.listClients(search, pageable))));
    }

    @Operation(summary = "Get client by ID", description = "Retrieves the full details of a single client by their UUID. Requires the 'client:read' permission.")
    @GetMapping("/{clientId}")
    @PreAuthorize("hasAuthority('client:read')")
    public ResponseEntity<ApiResponse<ClientResponse>> getClient(@PathVariable UUID clientId) {
        return ResponseEntity.ok(ApiResponse.ok(clientService.getClient(clientId)));
    }

    @Operation(summary = "Create client", description = "Creates a new client under the current tenant. Requires the 'client:write' permission.")
    @PostMapping
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<ClientResponse>> createClient(
            @Valid @RequestBody ClientRequest request) {
        ClientResponse created = clientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Client created", created));
    }

    @Operation(summary = "Update client", description = "Replaces all updatable fields of the specified client with the provided data. Requires the 'client:write' permission.")
    @PutMapping("/{clientId}")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<ClientResponse>> updateClient(
            @PathVariable UUID clientId,
            @Valid @RequestBody ClientRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Client updated", clientService.updateClient(clientId, request)));
    }

    @Operation(summary = "Delete client", description = "Permanently removes a client and all associated data. Requires the 'client:write' permission.")
    @DeleteMapping("/{clientId}")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<Void>> deleteClient(@PathVariable UUID clientId) {
        clientService.deleteClient(clientId);
        return ResponseEntity.ok(ApiResponse.ok("Client deleted"));
    }

    // ── Branches ──────────────────────────────────────────────────────

    @Operation(summary = "List branches", description = "Returns all branches belonging to the specified client. Requires the 'client:read' permission.")
    @GetMapping("/{clientId}/branches")
    @PreAuthorize("hasAuthority('client:read')")
    public ResponseEntity<ApiResponse<List<BranchResponse>>> listBranches(@PathVariable UUID clientId) {
        return ResponseEntity.ok(ApiResponse.ok(clientService.listBranches(clientId)));
    }

    @Operation(summary = "Create branch", description = "Adds a new branch location to the specified client. Requires the 'client:write' permission.")
    @PostMapping("/{clientId}/branches")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<BranchResponse>> createBranch(
            @PathVariable UUID clientId,
            @Valid @RequestBody BranchRequest request) {
        BranchResponse created = clientService.createBranch(clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Branch created", created));
    }

    @Operation(summary = "Update branch", description = "Updates the specified branch's fields. Supports partial updates; omitted fields are left unchanged. Requires the 'client:write' permission.")
    @PatchMapping("/{clientId}/branches/{branchId}")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<BranchResponse>> updateBranch(
            @PathVariable UUID clientId,
            @PathVariable UUID branchId,
            @Valid @RequestBody BranchRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Branch updated", clientService.updateBranch(clientId, branchId, request)));
    }

    @Operation(summary = "Delete branch", description = "Permanently removes a branch by its UUID. Requires the 'client:write' permission.")
    @DeleteMapping("/branches/{branchId}")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable UUID branchId) {
        clientService.deleteBranch(branchId);
        return ResponseEntity.ok(ApiResponse.ok("Branch deleted"));
    }

    // ── Contacts ──────────────────────────────────────────────────────

    @Operation(summary = "List contacts", description = "Returns all contacts for a client, primary first. Requires 'client:read'.")
    @GetMapping("/{clientId}/contacts")
    @PreAuthorize("hasAuthority('client:read')")
    public ResponseEntity<ApiResponse<List<ContactResponse>>> listContacts(@PathVariable UUID clientId) {
        return ResponseEntity.ok(ApiResponse.ok(clientService.listContacts(clientId)));
    }

    @Operation(summary = "Add contact", description = "Adds a new contact to the client. Requires 'client:write'.")
    @PostMapping("/{clientId}/contacts")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<ContactResponse>> addContact(
            @PathVariable UUID clientId,
            @Valid @RequestBody ContactRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Contact added", clientService.addContact(clientId, request)));
    }

    @Operation(summary = "Update contact", description = "Updates an existing contact. Requires 'client:write'.")
    @PutMapping("/{clientId}/contacts/{contactId}")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<ContactResponse>> updateContact(
            @PathVariable UUID clientId,
            @PathVariable UUID contactId,
            @Valid @RequestBody ContactRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Contact updated",
                clientService.updateContact(clientId, contactId, request)));
    }

    @Operation(summary = "Delete contact", description = "Removes a contact from the client. Requires 'client:write'.")
    @DeleteMapping("/{clientId}/contacts/{contactId}")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<Void>> deleteContact(
            @PathVariable UUID clientId,
            @PathVariable UUID contactId) {
        clientService.deleteContact(clientId, contactId);
        return ResponseEntity.ok(ApiResponse.ok("Contact removed"));
    }
}
