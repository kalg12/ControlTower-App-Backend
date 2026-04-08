package com.controltower.app.auth;

import com.controltower.app.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for authentication endpoints:
 *   - Onboarding (creates tenant + admin user)
 *   - Login (valid and invalid credentials)
 *   - Token refresh
 *   - Logout (invalidates refresh token)
 *   - Protected endpoint access with/without token
 *   - Password reset flow (token generation + redemption)
 */
@DirtiesContext
@TestMethodOrder(MethodOrderer.DisplayName.class)
class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired ObjectMapper mapper;

    // Slug used by onboard_success — intentionally different from SLUG
    // to avoid conflicts when other tests call ensureOnboarded() first.
    private static final String NEW_SLUG  = "auth-test-new";
    private static final String SLUG      = "auth-test";
    private static final String EMAIL     = "admin@auth-test.com";
    private static final String PASSWORD  = "Admin123!";

    // ── Onboarding ────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /tenants/onboard → 201 Created with admin tokens")
    void onboard_success() throws Exception {
        mvc.perform(post("/api/v1/tenants/onboard")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "tenantName",    "Auth New Tenant",
                    "tenantSlug",    NEW_SLUG,
                    "adminEmail",    "admin@auth-test-new.com",
                    "adminPassword", PASSWORD,
                    "adminFullName", "Auth Admin New"
                ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenant.slug").value(NEW_SLUG));
    }

    @Test
    @DisplayName("POST /tenants/onboard → 409 when slug already taken")
    void onboard_duplicateSlug_returns409() throws Exception {
        // First onboarding
        mvc.perform(post("/api/v1/tenants/onboard")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "tenantName",    "Dup Tenant",
                    "tenantSlug",    "dup-slug",
                    "adminEmail",    "admin@dup.com",
                    "adminPassword", PASSWORD,
                    "adminFullName", "Dup Admin"
                ))))
                .andExpect(status().isCreated());

        // Second onboarding with same slug must fail with 409 CONFLICT
        mvc.perform(post("/api/v1/tenants/onboard")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "tenantName",    "Dup Tenant 2",
                    "tenantSlug",    "dup-slug",
                    "adminEmail",    "admin2@dup.com",
                    "adminPassword", PASSWORD,
                    "adminFullName", "Dup Admin 2"
                ))))
                .andExpect(status().isConflict());
    }

    // ── Login ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/login → 200 with valid credentials returns tokens")
    void login_validCredentials_returnsTokens() throws Exception {
        ensureOnboarded();

        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "email",    EMAIL,
                    "password", PASSWORD
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken",  notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()));
    }

    @Test
    @DisplayName("POST /auth/login → 401 with wrong password")
    void login_wrongPassword_returns401() throws Exception {
        ensureOnboarded();

        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "email",    EMAIL,
                    "password", "WrongPassword999!"
                ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login → 401 with unknown email")
    void login_unknownEmail_returns401() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "email",    "nobody@nowhere.com",
                    "password", PASSWORD
                ))))
                .andExpect(status().isUnauthorized());
    }

    // ── Token refresh ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/refresh → 200 returns new token pair")
    void refresh_validToken_returnsNewPair() throws Exception {
        ensureOnboarded();

        String loginBody = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "email", EMAIL, "password", PASSWORD
                ))))
                .andReturn().getResponse().getContentAsString();

        @SuppressWarnings("unchecked")
        String refreshToken = (String) ((Map<String, Object>)
                mapper.readValue(loginBody, Map.class).get("data")).get("refreshToken");

        mvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken",  notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()));
    }

    // ── Logout ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/logout → refresh token invalidated afterwards")
    void logout_invalidatesRefreshToken() throws Exception {
        ensureOnboarded();

        String loginBody = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "email", EMAIL, "password", PASSWORD
                ))))
                .andReturn().getResponse().getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> data  = (Map<String, Object>) mapper.readValue(loginBody, Map.class).get("data");
        String accessToken  = (String) data.get("accessToken");
        String refreshToken = (String) data.get("refreshToken");

        // Logout
        mvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("refreshToken", refreshToken)))
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Refresh should now fail
        mvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    // ── Protected endpoint ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /users → 401 without token")
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /users → 200 with valid token")
    void protectedEndpoint_withToken_returns200() throws Exception {
        ensureOnboarded();
        String token = getToken();

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/users")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ── Password reset ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/forgot-password → always 200 (no user enumeration)")
    void forgotPassword_alwaysReturns200() throws Exception {
        mvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("email", "nonexistent@example.com"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /auth/reset-password → 400 with invalid token")
    void resetPassword_invalidToken_returns400() throws Exception {
        mvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "token",       "not-a-real-token",
                    "newPassword", "NewPassword123!"
                ))))
                .andExpect(status().isBadRequest());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private volatile boolean onboarded = false;

    private synchronized void ensureOnboarded() throws Exception {
        if (!onboarded) {
            mvc.perform(post("/api/v1/tenants/onboard")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(Map.of(
                        "tenantName",    "Auth Test Tenant",
                        "tenantSlug",    SLUG,
                        "adminEmail",    EMAIL,
                        "adminPassword", PASSWORD,
                        "adminFullName", "Auth Admin"
                    ))))
                    .andReturn();
            onboarded = true;
        }
    }

    private String getToken() throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "email", EMAIL, "password", PASSWORD
                ))))
                .andReturn().getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) mapper.readValue(body, Map.class).get("data");
        return (String) data.get("accessToken");
    }
}
