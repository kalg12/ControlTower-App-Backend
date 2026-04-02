package com.controltower.app.settings.api;

import com.controltower.app.settings.application.SettingsService;
import com.controltower.app.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Settings", description = "Per-user application settings")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

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
}
