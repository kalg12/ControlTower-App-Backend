package com.controltower.app.licenses.api;

import com.controltower.app.licenses.api.dto.ActivateLicenseRequest;
import com.controltower.app.licenses.api.dto.LicenseResponse;
import com.controltower.app.licenses.application.LicenseService;
import com.controltower.app.licenses.domain.PlanRepository;
import com.controltower.app.licenses.domain.Plan;
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

@Tag(name = "Licenses", description = "Client license and plan management")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/licenses")
@RequiredArgsConstructor
public class LicenseController {

    private final LicenseService licenseService;
    private final PlanRepository planRepository;

    @Operation(summary = "List licenses", description = "Returns a paginated list of all licenses within the current tenant. Requires the 'license:read' permission.")
    @GetMapping
    @PreAuthorize("hasAuthority('license:read')")
    public ResponseEntity<ApiResponse<PageResponse<LicenseResponse>>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(licenseService.listLicenses(pageable))));
    }

    @Operation(summary = "Activate license", description = "Creates and activates a new license for a client based on the requested plan. Requires the 'license:write' permission.")
    @PostMapping
    @PreAuthorize("hasAuthority('license:write')")
    public ResponseEntity<ApiResponse<LicenseResponse>> activate(
            @Valid @RequestBody ActivateLicenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(licenseService.activate(request)));
    }

    @Operation(summary = "Get license by ID", description = "Retrieves the details of a single license by its UUID. Requires the 'license:read' permission.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('license:read')")
    public ResponseEntity<ApiResponse<LicenseResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(licenseService.getLicense(id)));
    }

    @Operation(summary = "Get license by client", description = "Retrieves the active license associated with the specified client. Requires the 'license:read' permission.")
    @GetMapping("/clients/{clientId}")
    @PreAuthorize("hasAuthority('license:read')")
    public ResponseEntity<ApiResponse<LicenseResponse>> getByClient(@PathVariable UUID clientId) {
        return ResponseEntity.ok(ApiResponse.ok(licenseService.getLicenseByClient(clientId)));
    }

    @Operation(summary = "Suspend license", description = "Suspends an active license, blocking the client's access until the license is reactivated. Requires the 'license:write' permission.")
    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('license:write')")
    public ResponseEntity<ApiResponse<LicenseResponse>> suspend(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(licenseService.suspend(id)));
    }

    @Operation(summary = "Reactivate license", description = "Reactivates a suspended or expired license and optionally extends it by the given number of days. Requires the 'license:write' permission.")
    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAuthority('license:write')")
    public ResponseEntity<ApiResponse<LicenseResponse>> reactivate(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "30") int extensionDays) {
        return ResponseEntity.ok(ApiResponse.ok(licenseService.reactivate(id, extensionDays)));
    }

    @Operation(summary = "Cancel license", description = "Permanently cancels a license. This action cannot be undone. Requires the 'license:write' permission.")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('license:write')")
    public ResponseEntity<ApiResponse<LicenseResponse>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(licenseService.cancel(id)));
    }

    @Operation(summary = "Get license features", description = "Returns the list of feature flags enabled for the given license. Requires the 'license:read' permission.")
    @GetMapping("/{id}/features")
    @PreAuthorize("hasAuthority('license:read')")
    public ResponseEntity<ApiResponse<List<String>>> features(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(licenseService.getEnabledFeatures(id)));
    }

    // ── Plans (public catalog) ────────────────────────────────────────

    @Operation(summary = "List plans", description = "Returns all active subscription plans available for license activation. Requires the 'license:read' permission.")
    @GetMapping("/plans")
    @PreAuthorize("hasAuthority('license:read')")
    public ResponseEntity<ApiResponse<List<Plan>>> listPlans() {
        return ResponseEntity.ok(ApiResponse.ok(planRepository.findByActiveTrue()));
    }
}
