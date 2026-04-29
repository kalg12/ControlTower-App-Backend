package com.controltower.app.campaigns;

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
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext
class CampaignListIntegrationTest extends BaseIntegrationTest {

    @Autowired ObjectMapper mapper;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = TestDataFactory.onboardAndGetToken(mvc, "campaign-list", "admin@campaign-list.com", "Admin123!");
    }

    @Test
    @DisplayName("List campaigns returns 200 without search filter")
    void listWithoutSearch_returnsOk() throws Exception {
        createCampaign("Welcome campaign");

        mvc.perform(get("/api/v1/campaigns?page=0&size=20")
                .header("Authorization", TestDataFactory.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("List campaigns returns filtered results when search is provided")
    void listWithSearch_returnsOk() throws Exception {
        String id = createCampaign("Retention Blast");

        mvc.perform(get("/api/v1/campaigns?page=0&size=20&search=retention")
                .header("Authorization", TestDataFactory.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[0].id", is(id)))
                .andExpect(jsonPath("$.data.content[0].name", is("Retention Blast")));
    }

    @Test
    @DisplayName("Create campaign returns 400 when scheduledAt format is invalid")
    void createWithInvalidScheduledAt_returnsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/campaigns")
                .header("Authorization", TestDataFactory.bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "name", "Bad schedule",
                        "type", "EMAIL",
                        "subject", "Hello",
                        "body", "Campaign body",
                        "scheduledAt", "tomorrow-at-8"
                ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("List campaigns returns 400 for negative page")
    void listWithNegativePage_returnsBadRequest() throws Exception {
        mvc.perform(get("/api/v1/campaigns?page=-1&size=20")
                .header("Authorization", TestDataFactory.bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Update campaign returns 200 and persists changes")
    void updateCampaign_returnsUpdatedData() throws Exception {
        String id = createCampaign("Original Name");

        mvc.perform(patch("/api/v1/campaigns/{id}", id)
                .header("Authorization", TestDataFactory.bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "name", "Updated Name",
                        "subject", "Updated subject"
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(id)))
                .andExpect(jsonPath("$.data.name", is("Updated Name")))
                .andExpect(jsonPath("$.data.subject", is("Updated subject")));
    }

    @Test
    @DisplayName("Send campaign returns 200, second send returns 400")
    void sendCampaign_twice_returnsExpectedStatuses() throws Exception {
        String id = createCampaign("Send once campaign");

        mvc.perform(post("/api/v1/campaigns/{id}/send", id)
                .header("Authorization", TestDataFactory.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Campaign sent")));

        mvc.perform(post("/api/v1/campaigns/{id}/send", id)
                .header("Authorization", TestDataFactory.bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Campaign already sent")));
    }

    @Test
    @DisplayName("Delete campaign soft-deletes record and hides it from list")
    void deleteCampaign_hidesFromList() throws Exception {
        String id = createCampaign("Delete Me");

        mvc.perform(delete("/api/v1/campaigns/{id}", id)
                .header("Authorization", TestDataFactory.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Campaign deleted")));

        mvc.perform(get("/api/v1/campaigns?page=0&size=20&search=delete me")
                .header("Authorization", TestDataFactory.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalElements", is(0)));
    }

    @Test
    @DisplayName("Campaign endpoints return 401 without token")
    void campaignEndpoints_withoutToken_return403() throws Exception {
        String anyId = UUID.randomUUID().toString();

        mvc.perform(get("/api/v1/campaigns?page=0&size=20"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/campaigns")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "name", "No auth campaign",
                        "type", "EMAIL",
                        "subject", "Hello",
                        "body", "Campaign body"
                ))))
                .andExpect(status().isUnauthorized());

        mvc.perform(patch("/api/v1/campaigns/{id}", anyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("name", "Updated"))))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/campaigns/{id}/send", anyId))
                .andExpect(status().isUnauthorized());

        mvc.perform(delete("/api/v1/campaigns/{id}", anyId))
                .andExpect(status().isUnauthorized());
    }

    private String createCampaign(String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/campaigns")
                .header("Authorization", TestDataFactory.bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "name", name,
                        "type", "EMAIL",
                        "subject", "Hello",
                        "body", "Campaign body"
                ))))
                .andExpect(status().isCreated())
                .andReturn();

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>)
                mapper.readValue(result.getResponse().getContentAsString(), Map.class).get("data");
        return (String) data.get("id");
    }
}

