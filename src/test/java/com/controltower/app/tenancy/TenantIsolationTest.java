package com.controltower.app.tenancy;

import com.controltower.app.BaseIntegrationTest;
import com.controltower.app.util.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies that tenant data isolation is enforced:
 * a user from Tenant A must not see or modify resources belonging to Tenant B.
 */
@DirtiesContext
class TenantIsolationTest extends BaseIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() throws Exception {
        tokenA = TestDataFactory.onboardAndGetToken(mvc, "iso-tenant-a", "admin@iso-a.com", "Admin123!");
        tokenB = TestDataFactory.onboardAndGetToken(mvc, "iso-tenant-b", "admin@iso-b.com", "Admin123!");
    }

    // ── Clients ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Client created by Tenant A is not visible to Tenant B")
    void clientCreatedByTenantA_notVisibleToTenantB() throws Exception {
        // Tenant A creates a client
        MvcResult create = mvc.perform(post("/api/v1/clients")
                .header("Authorization", TestDataFactory.bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "name",    "Tenant A's Client",
                    "country", "Mexico"
                ))))
                .andExpect(status().isCreated())
                .andReturn();

        @SuppressWarnings("unchecked")
        String clientId = (String) ((Map<String, Object>)
                mapper.readValue(create.getResponse().getContentAsString(), Map.class)
                        .get("data")).get("id");

        // Tenant B tries to access it directly — should get 404
        mvc.perform(get("/api/v1/clients/" + clientId)
                .header("Authorization", TestDataFactory.bearer(tokenB)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Tenant B list clients → does not include Tenant A's clients")
    void listClients_tenantBDoesNotSeeTenantAClients() throws Exception {
        // Tenant A creates a client
        mvc.perform(post("/api/v1/clients")
                .header("Authorization", TestDataFactory.bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "name",    "Exclusive Client of A",
                    "country", "Mexico"
                ))))
                .andExpect(status().isCreated());

        // Tenant B lists clients — should see none of Tenant A's data
        MvcResult listResult = mvc.perform(get("/api/v1/clients")
                .header("Authorization", TestDataFactory.bearer(tokenB)))
                .andExpect(status().isOk())
                .andReturn();

        String body = listResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> page = (Map<String, Object>)
                mapper.readValue(body, Map.class).get("data");
        long totalElements = ((Number) page.get("totalElements")).longValue();

        // Tenant B should have 0 clients (they only created none)
        assert totalElements == 0 : "Tenant B should not see Tenant A's clients";
    }

    // ── Tickets ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Ticket created by Tenant A is not visible to Tenant B")
    void ticketCreatedByTenantA_notVisibleToTenantB() throws Exception {
        // Tenant A creates a ticket
        MvcResult create = mvc.perform(post("/api/v1/tickets")
                .header("Authorization", TestDataFactory.bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "title",       "Private ticket",
                    "description", "Only Tenant A should see this",
                    "priority",    "MEDIUM"
                ))))
                .andExpect(status().isCreated())
                .andReturn();

        @SuppressWarnings("unchecked")
        String ticketId = (String) ((Map<String, Object>)
                mapper.readValue(create.getResponse().getContentAsString(), Map.class)
                        .get("data")).get("id");

        // Tenant B tries to get it → 404
        mvc.perform(get("/api/v1/tickets/" + ticketId)
                .header("Authorization", TestDataFactory.bearer(tokenB)))
                .andExpect(status().isNotFound());
    }

    // ── Audit log ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Audit log is scoped to current tenant")
    void auditLog_scopedToTenant() throws Exception {
        // Tenant A creates a client (generates audit entry)
        mvc.perform(post("/api/v1/clients")
                .header("Authorization", TestDataFactory.bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("name", "Audit Client A"))))
                .andExpect(status().isCreated());

        // Tenant B's audit log should not contain Tenant A's entries
        MvcResult auditResult = mvc.perform(get("/api/v1/audit")
                .header("Authorization", TestDataFactory.bearer(tokenB)))
                .andExpect(status().isOk())
                .andReturn();

        String body = auditResult.getResponse().getContentAsString();
        // The response should be an empty or small set not containing "Audit Client A"
        assert !body.contains("Audit Client A")
                : "Tenant B should not see Tenant A's audit entries";
    }
}
