package com.controltower.app.tenancy.api;

import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import com.controltower.app.tenancy.api.dto.TenantRequest;
import com.controltower.app.tenancy.api.dto.TenantResponse;
import com.controltower.app.tenancy.application.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/**
 * Tenant management — super-admin only operations.
 */
@Tag(name = "Tenants", description = "Tenant management (super-admin only)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    @PreAuthorize("hasAuthority('tenant:read')")
    public ResponseEntity<ApiResponse<PageResponse<TenantResponse>>> listTenants(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(tenantService.listTenants(pageable))));
    }

    @GetMapping("/{tenantId}")
    @PreAuthorize("hasAuthority('tenant:read')")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenant(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.ok(tenantService.getTenant(tenantId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('tenant:write')")
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(
            @Valid @RequestBody TenantRequest request) {
        TenantResponse created = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Tenant created", created));
    }

    @PutMapping("/{tenantId}")
    @PreAuthorize("hasAuthority('tenant:write')")
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenant(
            @PathVariable UUID tenantId,
            @Valid @RequestBody TenantRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Tenant updated", tenantService.updateTenant(tenantId, request)));
    }

    @PostMapping("/{tenantId}/suspend")
    @PreAuthorize("hasAuthority('tenant:write')")
    public ResponseEntity<ApiResponse<Void>> suspendTenant(@PathVariable UUID tenantId) {
        tenantService.suspendTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.ok("Tenant suspended"));
    }

    @PostMapping("/{tenantId}/reactivate")
    @PreAuthorize("hasAuthority('tenant:write')")
    public ResponseEntity<ApiResponse<Void>> reactivateTenant(@PathVariable UUID tenantId) {
        tenantService.reactivateTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.ok("Tenant reactivated"));
    }

    // ── Config ───────────────────────────────────────────────────────

    @GetMapping("/{tenantId}/config")
    @PreAuthorize("hasAuthority('tenant:read')")
    public ResponseEntity<ApiResponse<Map<String, String>>> getConfig(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.ok(tenantService.getConfig(tenantId)));
    }

    @PutMapping("/{tenantId}/config/{key}")
    @PreAuthorize("hasAuthority('tenant:write')")
    public ResponseEntity<ApiResponse<Void>> setConfig(
            @PathVariable UUID tenantId,
            @PathVariable String key,
            @RequestBody String value) {
        tenantService.setConfig(tenantId, key, value);
        return ResponseEntity.ok(ApiResponse.ok("Config updated"));
    }
}
