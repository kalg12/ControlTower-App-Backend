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

    @GetMapping
    @PreAuthorize("hasAuthority('license:read')")
    public ResponseEntity<ApiResponse<PageResponse<LicenseResponse>>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(licenseService.listLicenses(pageable))));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('license:write')")
    public ResponseEntity<ApiResponse<LicenseResponse>> activate(
            @Valid @RequestBody ActivateLicenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(licenseService.activate(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('license:read')")
    public ResponseEntity<ApiResponse<LicenseResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(licenseService.getLicense(id)));
    }

    @GetMapping("/clients/{clientId}")
    @PreAuthorize("hasAuthority('license:read')")
    public ResponseEntity<ApiResponse<LicenseResponse>> getByClient(@PathVariable UUID clientId) {
        return ResponseEntity.ok(ApiResponse.ok(licenseService.getLicenseByClient(clientId)));
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('license:write')")
    public ResponseEntity<ApiResponse<LicenseResponse>> suspend(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(licenseService.suspend(id)));
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAuthority('license:write')")
    public ResponseEntity<ApiResponse<LicenseResponse>> reactivate(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "30") int extensionDays) {
        return ResponseEntity.ok(ApiResponse.ok(licenseService.reactivate(id, extensionDays)));
    }

    @GetMapping("/{id}/features")
    @PreAuthorize("hasAuthority('license:read')")
    public ResponseEntity<ApiResponse<List<String>>> features(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(licenseService.getEnabledFeatures(id)));
    }

    // ── Plans (public catalog) ────────────────────────────────────────

    @GetMapping("/plans")
    @PreAuthorize("hasAuthority('license:read')")
    public ResponseEntity<ApiResponse<List<Plan>>> listPlans() {
        return ResponseEntity.ok(ApiResponse.ok(planRepository.findByActiveTrue()));
    }
}
