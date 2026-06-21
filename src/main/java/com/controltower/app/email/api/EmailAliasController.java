package com.controltower.app.email.api;

import com.controltower.app.email.api.dto.AliasRequest;
import com.controltower.app.email.api.dto.AliasResponse;
import com.controltower.app.email.domain.EmailAlias;
import com.controltower.app.email.domain.EmailAliasRepository;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.tenancy.domain.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Email — Aliases", description = "Email address aliases linked to mailboxes and departments")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/email/aliases")
@RequiredArgsConstructor
public class EmailAliasController {

    private final EmailAliasRepository aliasRepo;

    @GetMapping
    @PreAuthorize("hasAuthority('email:read')")
    @Operation(summary = "List all active email aliases")
    public ResponseEntity<ApiResponse<List<AliasResponse>>> list() {
        UUID tenantId = TenantContext.getTenantId();
        List<AliasResponse> data = aliasRepo.findByTenantIdAndActiveTrue(tenantId)
            .stream().map(AliasResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('email:write')")
    @Operation(summary = "Create a new email alias")
    public ResponseEntity<ApiResponse<AliasResponse>> create(@Valid @RequestBody AliasRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        EmailAlias alias = new EmailAlias();
        alias.setTenantId(tenantId);
        alias.setMailboxId(req.mailboxId());
        alias.setName(req.name());
        alias.setAlias(req.alias().toLowerCase().trim());
        alias.setDepartmentId(req.departmentId());
        if (req.forwardTo() != null) {
            alias.setForwardTo(req.forwardTo().toArray(new String[0]));
        }
        aliasRepo.save(alias);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Alias created", AliasResponse.from(alias)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('email:write')")
    @Operation(summary = "Update an alias")
    public ResponseEntity<ApiResponse<AliasResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AliasRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        EmailAlias alias = aliasRepo.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Alias not found"));
        alias.setMailboxId(req.mailboxId());
        alias.setName(req.name());
        alias.setAlias(req.alias().toLowerCase().trim());
        alias.setDepartmentId(req.departmentId());
        if (req.forwardTo() != null) {
            alias.setForwardTo(req.forwardTo().toArray(new String[0]));
        }
        aliasRepo.save(alias);
        return ResponseEntity.ok(ApiResponse.ok("Alias updated", AliasResponse.from(alias)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('email:manage')")
    @Operation(summary = "Deactivate an alias")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        EmailAlias alias = aliasRepo.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Alias not found"));
        alias.setActive(false);
        aliasRepo.save(alias);
        return ResponseEntity.ok(ApiResponse.ok("Alias deactivated"));
    }
}
