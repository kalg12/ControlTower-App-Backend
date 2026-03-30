package com.controltower.app.identity.infrastructure.security;

import com.controltower.app.identity.domain.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Generates and validates JWT access and refresh tokens.
 *
 * Access token claims: userId, tenantId, email, roles, permissions
 * Refresh token claims: userId, tenantId, tokenId (for Redis revocation)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    private static final String CLAIM_TENANT_ID    = "tenantId";
    private static final String CLAIM_EMAIL        = "email";
    private static final String CLAIM_ROLES        = "roles";
    private static final String CLAIM_PERMISSIONS  = "permissions";
    private static final String CLAIM_TOKEN_ID     = "tokenId";
    private static final String CLAIM_TYPE         = "type";
    private static final String TYPE_ACCESS        = "access";
    private static final String TYPE_REFRESH       = "refresh";

    // ── Token generation ─────────────────────────────────────────────

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtProperties.getAccessTokenExpirationSeconds());

        Set<String> roleNames = new java.util.HashSet<>();
        user.getRoles().forEach(r -> roleNames.add(r.getCode()));

        return Jwts.builder()
                .subject(user.getId().toString())
                .claims(Map.of(
                    CLAIM_TENANT_ID,   user.getTenant().getId().toString(),
                    CLAIM_EMAIL,       user.getEmail(),
                    CLAIM_ROLES,       roleNames,
                    CLAIM_PERMISSIONS, user.getAllPermissions(),
                    CLAIM_TYPE,        TYPE_ACCESS
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(User user, String tokenId) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtProperties.getRefreshTokenExpirationSeconds());

        return Jwts.builder()
                .subject(user.getId().toString())
                .claims(Map.of(
                    CLAIM_TENANT_ID, user.getTenant().getId().toString(),
                    CLAIM_TOKEN_ID,  tokenId,
                    CLAIM_TYPE,      TYPE_REFRESH
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Token validation ─────────────────────────────────────────────

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID getUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public UUID getTenantId(String token) {
        return UUID.fromString(parseClaims(token).get(CLAIM_TENANT_ID, String.class));
    }

    public String getTokenId(String token) {
        return parseClaims(token).get(CLAIM_TOKEN_ID, String.class);
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
    }

    public long getRefreshTokenExpirationSeconds() {
        return jwtProperties.getRefreshTokenExpirationSeconds();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
            java.util.Base64.getEncoder().encodeToString(
                jwtProperties.getSecret().getBytes()
            )
        );
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
