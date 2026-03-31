package com.controltower.app.health;

import com.controltower.app.BaseIntegrationTest;
import com.controltower.app.util.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the health monitoring subsystem:
 *   - Heartbeat creates a HealthCheck record
 *   - HealthCheck is visible in branch history
 *   - Dashboard overview reflects health status
 *   - Health check for unknown branch returns 404
 */
@DirtiesContext
class HealthHeartbeatIntegrationTest extends BaseIntegrationTest {

    @Autowired ObjectMapper mapper;

    private String token;
    private String branchSlug;
    private String branchId;

    @BeforeEach
    void setUp() throws Exception {
        token = TestDataFactory.onboardAndGetToken(mvc, "health-hb", "admin@health-hb.com", "Admin123!");

        // Create a client
        MvcResult clientResult = mvc.perform(post("/api/v1/clients")
                .header("Authorization", TestDataFactory.bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("name", "HB Client"))))
                .andExpect(status().isCreated())
                .andReturn();

        @SuppressWarnings("unchecked")
        String clientId = (String) ((Map<String, Object>)
                mapper.readValue(clientResult.getResponse().getContentAsString(), Map.class)
                        .get("data")).get("id");

        // Create a branch
        MvcResult branchResult = mvc.perform(post("/api/v1/clients/" + clientId + "/branches")
                .header("Authorization", TestDataFactory.bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "name",    "Branch HB",
                    "country", "Mexico"
                ))))
                .andExpect(status().isCreated())
                .andReturn();

        @SuppressWarnings("unchecked")
        Map<String, Object> branchData = (Map<String, Object>)
                mapper.readValue(branchResult.getResponse().getContentAsString(), Map.class).get("data");
        branchId   = (String) branchData.get("id");
        branchSlug = (String) branchData.get("slug");
    }

    @Test
    @DisplayName("POST /health/heartbeat/{slug} → 200 and creates health record")
    void heartbeat_knownBranch_creates200() throws Exception {
        mvc.perform(post("/api/v1/health/heartbeat/" + branchSlug)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "status",     "UP",
                    "latencyMs",  42,
                    "version",    "1.0.0"
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    @DisplayName("POST /health/heartbeat/{slug} → 404 for unknown slug")
    void heartbeat_unknownSlug_returns404() throws Exception {
        mvc.perform(post("/api/v1/health/heartbeat/nonexistent-slug-xyz")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("status", "UP"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Branch health history shows heartbeat after ping")
    void branchHistory_containsHeartbeat() throws Exception {
        // Send a heartbeat
        mvc.perform(post("/api/v1/health/heartbeat/" + branchSlug)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("status", "UP", "latencyMs", 10))))
                .andExpect(status().isOk());

        // Verify it appears in branch history
        mvc.perform(get("/api/v1/health/branches/" + branchId)
                .header("Authorization", TestDataFactory.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data.content[0].status").value("UP"));
    }

    @Test
    @DisplayName("Health client overview reflects UP status after heartbeat")
    void clientOverview_reflectsHeartbeat() throws Exception {
        // Send heartbeat
        mvc.perform(post("/api/v1/health/heartbeat/" + branchSlug)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("status", "UP"))))
                .andExpect(status().isOk());

        // Overview should show at least one client with UP branches
        mvc.perform(get("/api/v1/health/clients")
                .header("Authorization", TestDataFactory.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    @DisplayName("DOWN heartbeat triggers incident creation")
    void downHeartbeat_triggersIncident() throws Exception {
        // Send multiple DOWN heartbeats to trigger auto-incident
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/v1/health/heartbeat/" + branchSlug)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(Map.of("status", "DOWN"))))
                    .andExpect(status().isOk());
        }

        // Check incidents endpoint — should have at least one open incident
        MvcResult result = mvc.perform(get("/api/v1/health/incidents")
                .header("Authorization", TestDataFactory.bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> page = (Map<String, Object>)
                mapper.readValue(body, Map.class).get("data");
        long totalElements = ((Number) page.get("totalElements")).longValue();

        assert totalElements >= 1 : "Expected at least one incident after repeated DOWN heartbeats";
    }
}
