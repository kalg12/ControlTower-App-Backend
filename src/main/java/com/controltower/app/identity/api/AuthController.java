package com.controltower.app.identity.api;

import com.controltower.app.identity.api.dto.*;
import com.controltower.app.identity.application.AuthService;
import com.controltower.app.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints — all public (no JWT required).
 */
@Tag(name = "Auth", description = "Authentication — login, token refresh, logout")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Login with email + password. Returns access token and refresh token. */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    /** Rotate tokens using a valid refresh token. */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshRequest request) {
        LoginResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok("Tokens refreshed", response));
    }

    /** Invalidate the refresh token (logout). */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }

    /** Initiate password reset — always returns 200 to prevent user enumeration. */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("If the email exists, a reset link has been sent"));
    }

    /** Reset password using a valid token. */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully"));
    }

    // ── 2FA endpoints ────────────────────────────────────────────────

    /** Generates a TOTP secret and QR URL. Requires authentication. */
    @PostMapping("/2fa/setup")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<TotpSetupResponse>> setup2fa(
            @AuthenticationPrincipal UserDetails principal) {
        java.util.UUID userId = java.util.UUID.fromString(principal.getUsername());
        TotpSetupResponse response = authService.setupTotp(userId);
        return ResponseEntity.ok(ApiResponse.ok("TOTP secret generated", response));
    }

    /** Enables 2FA after verifying the first code. Requires authentication. */
    @PostMapping("/2fa/enable")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> enable2fa(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody TotpCodeRequest request) {
        java.util.UUID userId = java.util.UUID.fromString(principal.getUsername());
        authService.enableTotp(userId, request.getCode());
        return ResponseEntity.ok(ApiResponse.ok("2FA enabled successfully"));
    }

    /** Disables 2FA after confirming with a valid TOTP code. Requires authentication. */
    @PostMapping("/2fa/disable")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> disable2fa(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody TotpCodeRequest request) {
        java.util.UUID userId = java.util.UUID.fromString(principal.getUsername());
        authService.disableTotp(userId, request.getCode());
        return ResponseEntity.ok(ApiResponse.ok("2FA disabled successfully"));
    }

    /** Verifies MFA token + TOTP code and returns full access + refresh tokens. Public. */
    @PostMapping("/2fa/verify")
    public ResponseEntity<ApiResponse<LoginResponse>> verify2fa(
            @Valid @RequestBody TotpVerifyRequest request) {
        LoginResponse response = authService.verifyMfa(request.getMfaToken(), request.getCode());
        return ResponseEntity.ok(ApiResponse.ok("MFA verification successful", response));
    }
}
