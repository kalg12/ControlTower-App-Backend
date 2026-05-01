package com.controltower.app.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Helper utilities shared across integration tests.
 * Provides methods to onboard tenants and obtain JWT tokens via HTTP calls.
 */
public final class TestDataFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestDataFactory() {}

    /**
     * Calls POST /api/v1/tenants/onboard to create a new tenant + admin user.
     * Returns the admin's access token.
     */
    public static String onboardAndGetToken(MockMvc mvc, String slug, String email, String password)
            throws Exception {

        String resolvedSlug = slug + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);

        // 1. Onboard (idempotent: if tenant already exists, skip and proceed to login)
        MvcResult onboardResult = mvc.perform(post("/api/v1/tenants/onboard")
                .contentType(MediaType.APPLICATION_JSON)
                .content(MAPPER.writeValueAsString(Map.of(
                    "tenantName",     "Test Tenant " + resolvedSlug,
                    "tenantSlug",     resolvedSlug,
                    "adminEmail",     email,
                    "adminPassword",  password,
                    "adminFullName",  "Admin " + resolvedSlug
                ))))
                .andReturn();
        int onboardStatus = onboardResult.getResponse().getStatus();

        if (onboardStatus != 201 && onboardStatus != 200 && onboardStatus != 409) {
            throw new AssertionError(
                "Onboarding failed for slug='" + resolvedSlug + "' with status " + onboardStatus
                + ". Body: " + onboardResult.getResponse().getContentAsString()
            );
        }

        // 2. Login and return access token
        return login(mvc, email, password);
    }

    /**
     * Calls POST /api/v1/auth/login and returns the access token string.
     */
    public static String login(MockMvc mvc, String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(MAPPER.writeValueAsString(Map.of(
                    "email",    email,
                    "password", password
                ))))
                .andReturn();

        int loginStatus = result.getResponse().getStatus();
        if (loginStatus != 200) {
            throw new AssertionError(
                "Login failed for email='" + email + "' with status " + loginStatus
                + ". Body: " + result.getResponse().getContentAsString()
            );
        }

        String body = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>)
                ((Map<String, Object>) MAPPER.readValue(body, Map.class)).get("data");
        return (String) data.get("accessToken");
    }

    /**
     * Returns an Authorization header value for the given token.
     */
    public static String bearer(String token) {
        return "Bearer " + token;
    }
}
