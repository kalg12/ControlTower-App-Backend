package com.controltower.app.identity.application;

import com.controltower.app.identity.api.dto.LoginRequest;
import com.controltower.app.identity.api.dto.LoginResponse;
import com.controltower.app.identity.api.dto.RefreshRequest;
import com.controltower.app.identity.domain.PasswordResetToken;
import com.controltower.app.identity.domain.PasswordResetTokenRepository;
import com.controltower.app.identity.domain.User;
import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.identity.infrastructure.security.JwtTokenProvider;
import com.controltower.app.shared.exception.ControlTowerException;
import com.controltower.app.shared.infrastructure.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Handles authentication: login, token refresh, and logout.
 *
 * Refresh tokens are stored in Redis under key:
 *   refresh_token:{userId}:{tokenId}  →  "valid"
 * Logout deletes the key, making the refresh token permanently invalid.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TotpService totpService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final String REFRESH_KEY_PREFIX = "refresh_token:";

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // Verify credentials manually — UserDetailsServiceImpl uses UUID as username,
        // so the authenticationManager flow is used only for filter-based auth (JWT).
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new ControlTowerException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ControlTowerException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new ControlTowerException("Account is not active", HttpStatus.FORBIDDEN);
        }

        // If 2FA is enabled, return an MFA token instead of full tokens
        if (user.isTotpEnabled()) {
            String mfaToken = jwtTokenProvider.generateMfaToken(user.getId());
            log.info("User {} requires 2FA verification", user.getEmail());
            return LoginResponse.builder()
                    .mfaToken(mfaToken)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .requiresMfa(true)
                    .build();
        }

        String tokenId = UUID.randomUUID().toString();
        String accessToken  = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user, tokenId);

        // Store refresh token reference in Redis
        String redisKey = REFRESH_KEY_PREFIX + user.getId() + ":" + tokenId;
        redisTemplate.opsForValue().set(
            redisKey, "valid",
            Duration.ofSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds())
        );

        log.info("User {} logged in successfully", user.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .tenantId(user.getTenant().getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }

    @Transactional(readOnly = true)
    public LoginResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new ControlTowerException("Invalid refresh token", HttpStatus.UNAUTHORIZED);
        }

        UUID userId  = jwtTokenProvider.getUserId(refreshToken);
        String tokenId = jwtTokenProvider.getTokenId(refreshToken);

        // Verify the refresh token is still valid in Redis
        String redisKey = REFRESH_KEY_PREFIX + userId + ":" + tokenId;
        String stored = redisTemplate.opsForValue().get(redisKey);
        if (stored == null) {
            throw new ControlTowerException("Refresh token has been revoked", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ControlTowerException("User not found", HttpStatus.UNAUTHORIZED));

        // Rotate: delete old token, issue new pair
        redisTemplate.delete(redisKey);

        String newTokenId    = UUID.randomUUID().toString();
        String newAccess     = jwtTokenProvider.generateAccessToken(user);
        String newRefresh    = jwtTokenProvider.generateRefreshToken(user, newTokenId);

        String newRedisKey = REFRESH_KEY_PREFIX + userId + ":" + newTokenId;
        redisTemplate.opsForValue().set(
            newRedisKey, "valid",
            Duration.ofSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds())
        );

        return LoginResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .userId(user.getId())
                .tenantId(user.getTenant().getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }

    public void logout(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            return; // Invalid token — treat as already logged out
        }
        UUID userId  = jwtTokenProvider.getUserId(refreshToken);
        String tokenId = jwtTokenProvider.getTokenId(refreshToken);
        redisTemplate.delete(REFRESH_KEY_PREFIX + userId + ":" + tokenId);
        log.info("User {} logged out, refresh token revoked", userId);
    }

    /**
     * Initiates a password reset. Always returns success to prevent user enumeration.
     */
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmailAndDeletedAtIsNull(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUserId(user.getId());
            resetToken.setToken(token);
            resetToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
            passwordResetTokenRepository.save(resetToken);

            String resetLink = baseUrl + "/reset-password?token=" + token;
            emailService.sendPasswordReset(email, resetLink);
            log.info("Password reset token created for user {}", user.getId());
        });
    }

    /**
     * Resets the user password using a valid token.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenAndUsedAtIsNull(token)
                .orElseThrow(() -> new ControlTowerException("Invalid or expired reset token", HttpStatus.BAD_REQUEST));

        if (resetToken.isExpired()) {
            throw new ControlTowerException("Reset token has expired", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(resetToken.getUserId())
                .orElseThrow(() -> new ControlTowerException("User not found", HttpStatus.NOT_FOUND));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successfully for user {}", user.getId());
    }

    /**
     * Generates a new TOTP secret and QR URL for the current user.
     * Saves the secret but does NOT enable 2FA yet (requires confirmation via enable()).
     */
    @Transactional
    public com.controltower.app.identity.api.dto.TotpSetupResponse setupTotp(UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ControlTowerException("User not found", HttpStatus.NOT_FOUND));
        String secret = totpService.generateSecret();
        user.setTotpSecret(secret);
        userRepository.save(user);
        String qrUrl = totpService.getQrUrl(user.getEmail(), secret);
        return com.controltower.app.identity.api.dto.TotpSetupResponse.builder()
                .secret(secret)
                .qrUrl(qrUrl)
                .build();
    }

    /**
     * Enables 2FA after verifying the first TOTP code.
     */
    @Transactional
    public void enableTotp(UUID userId, int code) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ControlTowerException("User not found", HttpStatus.NOT_FOUND));
        if (user.getTotpSecret() == null) {
            throw new ControlTowerException("2FA setup not started — call /auth/2fa/setup first", HttpStatus.BAD_REQUEST);
        }
        if (!totpService.verifyCode(user.getTotpSecret(), code)) {
            throw new ControlTowerException("Invalid TOTP code", HttpStatus.BAD_REQUEST);
        }
        user.setTotpEnabled(true);
        userRepository.save(user);
        log.info("2FA enabled for user {}", userId);
    }

    /**
     * Disables 2FA after verifying a valid TOTP code.
     */
    @Transactional
    public void disableTotp(UUID userId, int code) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ControlTowerException("User not found", HttpStatus.NOT_FOUND));
        if (!user.isTotpEnabled()) {
            throw new ControlTowerException("2FA is not enabled", HttpStatus.BAD_REQUEST);
        }
        if (!totpService.verifyCode(user.getTotpSecret(), code)) {
            throw new ControlTowerException("Invalid TOTP code", HttpStatus.BAD_REQUEST);
        }
        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        userRepository.save(user);
        log.info("2FA disabled for user {}", userId);
    }

    /**
     * Completes MFA verification: validates mfaToken + TOTP code, returns full tokens.
     */
    @Transactional(readOnly = true)
    public LoginResponse verifyMfa(String mfaToken, int code) {
        if (!jwtTokenProvider.validateToken(mfaToken) || !jwtTokenProvider.isMfaPending(mfaToken)) {
            throw new ControlTowerException("Invalid MFA token", HttpStatus.UNAUTHORIZED);
        }

        UUID userId = jwtTokenProvider.getUserId(mfaToken);
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ControlTowerException("User not found", HttpStatus.UNAUTHORIZED));

        if (!totpService.verifyCode(user.getTotpSecret(), code)) {
            throw new ControlTowerException("Invalid TOTP code", HttpStatus.UNAUTHORIZED);
        }

        String tokenId = UUID.randomUUID().toString();
        String accessToken  = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user, tokenId);

        String redisKey = REFRESH_KEY_PREFIX + user.getId() + ":" + tokenId;
        redisTemplate.opsForValue().set(
            redisKey, "valid",
            Duration.ofSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds())
        );

        log.info("User {} completed MFA verification", user.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .tenantId(user.getTenant().getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }
}
