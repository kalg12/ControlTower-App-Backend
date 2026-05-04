package com.controltower.app.settings.api;

import com.controltower.app.identity.domain.Tenant;
import com.controltower.app.identity.domain.TenantRepository;
import com.controltower.app.settings.application.SettingsService;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.tenancy.domain.TenantConfig;
import com.controltower.app.tenancy.domain.TenantConfigRepository;
import com.controltower.app.tenancy.domain.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Settings", description = "Per-user and per-tenant application settings")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final TenantConfigRepository tenantConfigRepository;
    private final TenantRepository tenantRepository;

    @Operation(summary = "Get notification preferences")
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotificationPreferences(
            @AuthenticationPrincipal UserDetails principal) {
        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(settingsService.getNotificationPreferences(userId)));
    }

    @Operation(summary = "Save notification preferences")
    @PutMapping("/notifications")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveNotificationPreferences(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody Map<String, Object> prefs) {
        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(settingsService.saveNotificationPreferences(userId, prefs)));
    }

    @Operation(summary = "Get tenant configuration settings")
    @GetMapping("/tenant")
    public ResponseEntity<ApiResponse<Map<String, String>>> getTenantSettings() {
        UUID tenantId = TenantContext.getTenantId();
        Map<String, String> result = new HashMap<>();
        tenantConfigRepository.findByTenantId(tenantId)
                .forEach(cfg -> result.put(cfg.getKey(), cfg.getValue()));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "Update tenant configuration settings")
    @PutMapping("/tenant")
    public ResponseEntity<ApiResponse<Void>> updateTenantSettings(
            @RequestBody Map<String, String> settings) {
        UUID tenantId = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        settings.forEach((key, value) -> {
            TenantConfig cfg = tenantConfigRepository.findByTenantIdAndKey(tenantId, key)
                    .orElseGet(() -> {
                        TenantConfig c = new TenantConfig();
                        c.setTenant(tenant);
                        c.setKey(key);
                        return c;
                    });
            cfg.setValue(value);
            cfg.setUpdatedAt(Instant.now());
            tenantConfigRepository.save(cfg);
        });
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
