package com.controltower.app.identity.api;

import com.controltower.app.identity.api.dto.*;
import com.controltower.app.identity.application.AuthService;
import com.controltower.app.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Login", description = "Authenticates a user with email and password. Returns a short-lived access token and a refresh token on success.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    /** Rotate tokens using a valid refresh token. */
    @Operation(summary = "Refresh tokens", description = "Issues a new access token and rotates the refresh token. The old refresh token is invalidated after use.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshRequest request) {
        LoginResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok("Tokens refreshed", response));
    }

    /** Invalidate the refresh token (logout). */
    @Operation(summary = "Logout", description = "Invalidates the provided refresh token, effectively ending the user's session.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }

    /** Initiate password reset — always returns 200 to prevent user enumeration. */
    @Operation(summary = "Forgot password", description = "Sends a password reset link to the given email address if it exists. Always returns 200 to prevent user enumeration.")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("If the email exists, a reset link has been sent"));
    }

    /** Reset password using a valid token. */
    @Operation(summary = "Reset password", description = "Sets a new password for the account associated with the one-time reset token. The token is invalidated after use.")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully"));
    }

    /** Changes password for the authenticated user. */
    @Operation(summary = "Change password", description = "Requires current password. Requires a valid bearer token.")
    @PostMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        java.util.UUID userId = java.util.UUID.fromString(principal.getUsername());
        authService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
    }

    // ── 2FA endpoints ────────────────────────────────────────────────

    /** Generates a TOTP secret and QR URL. Requires authentication. */
    @Operation(summary = "Set up 2FA", description = "Generates a TOTP secret and provisioning QR code URL for the authenticated user. Requires a valid bearer token.")
    @PostMapping("/2fa/setup")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<TotpSetupResponse>> setup2fa(
            @AuthenticationPrincipal UserDetails principal) {
        java.util.UUID userId = java.util.UUID.fromString(principal.getUsername());
        TotpSetupResponse response = authService.setupTotp(userId);
        return ResponseEntity.ok(ApiResponse.ok("TOTP secret generated", response));
    }

    /** Enables 2FA after verifying the first code. Requires authentication. */
    @Operation(summary = "Enable 2FA", description = "Enables two-factor authentication for the authenticated user after verifying the first TOTP code. Requires a valid bearer token.")
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
    @Operation(summary = "Disable 2FA", description = "Disables two-factor authentication for the authenticated user after confirming with a valid TOTP code. Requires a valid bearer token.")
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
    @Operation(summary = "Verify 2FA", description = "Completes the MFA login flow by verifying the temporary MFA token and TOTP code. Returns full access and refresh tokens on success.")
    @PostMapping("/2fa/verify")
    public ResponseEntity<ApiResponse<LoginResponse>> verify2fa(
            @Valid @RequestBody TotpVerifyRequest request) {
        LoginResponse response = authService.verifyMfa(request.getMfaToken(), request.getCode());
        return ResponseEntity.ok(ApiResponse.ok("MFA verification successful", response));
    }
}
