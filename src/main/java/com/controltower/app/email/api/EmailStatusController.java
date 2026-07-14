package com.controltower.app.email.api;

import com.controltower.app.identity.domain.User;
import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.shared.config.ResendProperties;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.shared.infrastructure.EmailService;
import com.controltower.app.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Email", description = "Outbound email status (Resend)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
public class EmailStatusController {

    private final ResendProperties resendProperties;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @Operation(summary = "Get email sending status", description = "Whether Resend is configured (RESEND_API_KEY set) and the configured sender.")
    @GetMapping("/status")
    @PreAuthorize("hasAuthority('email:read')")
    public ResponseEntity<ApiResponse<EmailStatusResponse>> getStatus() {
        boolean configured = resendProperties.getApiKey() != null && !resendProperties.getApiKey().isBlank();
        return ResponseEntity.ok(ApiResponse.ok(new EmailStatusResponse(
                configured, resendProperties.getFromEmail(), resendProperties.getFromName())));
    }

    @Operation(summary = "Send a test email", description = "Sends a test email to the current user's address via Resend.")
    @PostMapping("/test-send")
    @PreAuthorize("hasAuthority('email:manage')")
    public ResponseEntity<ApiResponse<Void>> testSend(@AuthenticationPrincipal UserDetails principal) {
        UUID userId = UUID.fromString(principal.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        emailService.sendTest(user.getEmail(), user.getFullName());
        return ResponseEntity.ok(ApiResponse.ok("Test email queued"));
    }

    public record EmailStatusResponse(boolean configured, String fromEmail, String fromName) {}
}
