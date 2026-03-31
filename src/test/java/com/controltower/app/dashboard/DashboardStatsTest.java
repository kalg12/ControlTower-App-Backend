package com.controltower.app.dashboard;

import com.controltower.app.BaseIntegrationTest;
import com.controltower.app.util.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the dashboard stats endpoint.
 */
@DirtiesContext
class DashboardStatsTest extends BaseIntegrationTest {

    @Autowired ObjectMapper mapper;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = TestDataFactory.onboardAndGetToken(mvc, "dash-test", "admin@dash-test.com", "Admin123!");
    }

    @Test
    @DisplayName("GET /dashboard → 401 without token")
    void dashboard_withoutToken_returns401() throws Exception {
        mvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /dashboard → 200 with all expected fields")
    void dashboard_withToken_returnsAllFields() throws Exception {
        mvc.perform(get("/api/v1/dashboard")
                .header("Authorization", TestDataFactory.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalClients",       notNullValue()))
                .andExpect(jsonPath("$.data.activeBranches",     notNullValue()))
                .andExpect(jsonPath("$.data.branchesUp",         notNullValue()))
                .andExpect(jsonPath("$.data.branchesDown",       notNullValue()))
                .andExpect(jsonPath("$.data.openIncidents",      notNullValue()))
                .andExpect(jsonPath("$.data.openTickets",        notNullValue()))
                .andExpect(jsonPath("$.data.slaBreachedTickets", notNullValue()))
                .andExpect(jsonPath("$.data.activeLicenses",     notNullValue()))
                .andExpect(jsonPath("$.data.trialLicenses",      notNullValue()))
                .andExpect(jsonPath("$.data.unreadNotifications",notNullValue()));
    }

    @Test
    @DisplayName("After creating a client, dashboard totalClients increases")
    void dashboard_totalClients_incrementsAfterCreate() throws Exception {
        // Initial count
        String beforeBody = mvc.perform(get("/api/v1/dashboard")
                .header("Authorization", TestDataFactory.bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        @SuppressWarnings("unchecked")
        long before = ((Number) ((Map<String, Object>)
                mapper.readValue(beforeBody, Map.class).get("data")).get("totalClients")).longValue();

        // Create a client
        mvc.perform(post("/api/v1/clients")
                .header("Authorization", TestDataFactory.bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("name", "Dashboard Client"))))
                .andExpect(status().isCreated());

        // Check dashboard again
        mvc.perform(get("/api/v1/dashboard")
                .header("Authorization", TestDataFactory.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalClients", is((int)(before + 1))));
    }
}
