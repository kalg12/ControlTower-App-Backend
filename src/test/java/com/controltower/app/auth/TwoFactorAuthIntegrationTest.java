package com.controltower.app.auth;

import com.controltower.app.BaseIntegrationTest;
import com.controltower.app.util.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext
class TwoFactorAuthIntegrationTest extends BaseIntegrationTest {

    @Autowired ObjectMapper mapper;

    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();

    private String slug;
    private String email;
    private final String password = "Admin123!";
    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        slug = "twofa-" + System.currentTimeMillis();
        email = "admin@" + slug + ".com";
        accessToken = TestDataFactory.onboardAndGetToken(mvc, slug, email, password);
    }

    @Test
    @DisplayName("2FA setup is idempotent and does not rotate the secret after enable")
    void setupAfterEnable_reusesExistingSecret() throws Exception {
        Map<String, Object> firstSetup = setup2fa(accessToken);
        String originalSecret = (String) firstSetup.get("secret");

        mvc.perform(post("/api/v1/auth/2fa/enable")
                .header("Authorization", TestDataFactory.bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("code", currentCode(originalSecret)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        mvc.perform(get("/api/v1/auth/2fa/status")
                .header("Authorization", TestDataFactory.bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.enabled", is(true)))
                .andExpect(jsonPath("$.data.setupStarted", is(true)));

        mvc.perform(post("/api/v1/auth/2fa/setup")
                .header("Authorization", TestDataFactory.bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.secret", is(originalSecret)))
                .andExpect(jsonPath("$.data.enabled", is(true)));
    }

    @Test
    @DisplayName("Enabled 2FA login can still be verified after revisiting setup")
    void verifyMfa_usesOriginalSecretAfterReopeningSetup() throws Exception {
        Map<String, Object> firstSetup = setup2fa(accessToken);
        String originalSecret = (String) firstSetup.get("secret");

        mvc.perform(post("/api/v1/auth/2fa/enable")
                .header("Authorization", TestDataFactory.bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("code", currentCode(originalSecret)))))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/auth/2fa/setup")
                .header("Authorization", TestDataFactory.bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.secret", is(originalSecret)));

        MvcResult loginResult = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "email", email,
                        "password", password
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiresMfa", is(true)))
                .andExpect(jsonPath("$.data.mfaToken", notNullValue()))
                .andReturn();

        @SuppressWarnings("unchecked")
        Map<String, Object> loginData = (Map<String, Object>) mapper.readValue(
                loginResult.getResponse().getContentAsString(), Map.class).get("data");
        String mfaToken = (String) loginData.get("mfaToken");

        mvc.perform(post("/api/v1/auth/2fa/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "mfaToken", mfaToken,
                        "code", currentCode(originalSecret)
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.data.totpEnabled", is(true)));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> setup2fa(String token) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/2fa/setup")
                .header("Authorization", TestDataFactory.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andReturn();

        return (Map<String, Object>) mapper.readValue(result.getResponse().getContentAsString(), Map.class).get("data");
    }

    private int currentCode(String secret) {
        return googleAuthenticator.getTotpPassword(secret);
    }
}

