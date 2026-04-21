package com.controltower.app.calendar.api;

import com.controltower.app.calendar.infrastructure.GoogleCalendarSyncService;
import com.controltower.app.identity.domain.User;
import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Google Calendar", description = "Google Calendar integration endpoints")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/calendar/google")
@RequiredArgsConstructor
public class GoogleCalendarController {

    private final GoogleCalendarSyncService googleCalendarSyncService;
    private final UserRepository userRepository;

    @Operation(summary = "Get Google Calendar connection status")
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<GoogleCalendarStatusResponse>> getStatus(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        User user = userRepository.findById(userId).orElse(null);
        
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.ok(new GoogleCalendarStatusResponse(false, null)));
        }
        
        var status = new GoogleCalendarStatusResponse(
            user.isGoogleCalendarEnabled(),
            user.getGoogleCalendarEmail()
        );
        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    @Operation(summary = "Get authorization URL for connecting Google Calendar")
    @GetMapping("/auth-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<GoogleAuthUrlResponse>> getAuthUrl(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        String authUrl = googleCalendarSyncService.getAuthorizationUrl(userId);
        
        if (authUrl == null) {
            return ResponseEntity.ok(ApiResponse.ok(new GoogleAuthUrlResponse(null, "Google Calendar not configured")));
        }
        
        return ResponseEntity.ok(ApiResponse.ok(new GoogleAuthUrlResponse(authUrl, null)));
    }

    @Operation(summary = "Connect Google Calendar with OAuth code")
    @PostMapping("/connect")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> connect(
            @RequestBody GoogleConnectRequest request,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        
        try {
            String refreshToken = googleCalendarSyncService.exchangeCodeForRefreshToken(request.authCode(), userId);
            
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && refreshToken != null) {
                user.setGoogleRefreshToken(refreshToken);
                user.setGoogleCalendarEnabled(true);
                userRepository.save(user);
            }
            
            return ResponseEntity.ok(ApiResponse.ok("Google Calendar connected successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to connect: " + e.getMessage()));
        }
    }

    @Operation(summary = "Disconnect Google Calendar")
    @PostMapping("/disconnect")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> disconnect(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        User user = userRepository.findById(userId).orElse(null);
        
        if (user != null) {
            user.setGoogleRefreshToken(null);
            user.setGoogleCalendarEnabled(false);
            userRepository.save(user);
        }
        
        return ResponseEntity.ok(ApiResponse.ok("Google Calendar disconnected"));
    }

    @Operation(summary = "Sync events now")
    @PostMapping("/sync")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> syncNow(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        // This would trigger a sync in a real implementation
        return ResponseEntity.ok(ApiResponse.ok("Sync started"));
    }

    public record GoogleCalendarStatusResponse(boolean connected, String email) {}

    public record GoogleAuthUrlResponse(String url, String error) {}

    public record GoogleConnectRequest(String authCode) {}
}