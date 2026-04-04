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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

