package com.controltower.app.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

        // 1. Onboard (idempotent: if tenant already exists, skip and proceed to login)
        int onboardStatus = mvc.perform(post("/api/v1/tenants/onboard")
                .contentType(MediaType.APPLICATION_JSON)
                .content(MAPPER.writeValueAsString(Map.of(
                    "tenantName",     "Test Tenant " + slug,
                    "tenantSlug",     slug,
                    "adminEmail",     email,
                    "adminPassword",  password,
                    "adminFullName",  "Admin " + slug
                ))))
                .andReturn().getResponse().getStatus();

        if (onboardStatus != 201 && onboardStatus != 200) {
            // Tenant already exists from a previous @BeforeEach call — just login
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
                .andExpect(status().isOk())
                .andReturn();

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
