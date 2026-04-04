package com.controltower.app.support;

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
class TicketListIntegrationTest extends BaseIntegrationTest {

    @Autowired ObjectMapper mapper;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = TestDataFactory.onboardAndGetToken(mvc, "ticket-list", "admin@ticket-list.com", "Admin123!");
    }

    @Test
    @DisplayName("List tickets returns 200 without filters")
    void listWithoutFilters_returnsOk() throws Exception {
        createTicket("List baseline", "OPEN", "MEDIUM");

        mvc.perform(get("/api/v1/tickets?page=0&size=20")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("List tickets supports enum filters without server errors")
    void listWithEnumFilters_returnsOk() throws Exception {
        String id = createTicket("Filtered ticket", "OPEN", "HIGH");

        mvc.perform(get("/api/v1/tickets?page=0&size=20&status=OPEN&priority=HIGH")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[0].id", is(id)))
                .andExpect(jsonPath("$.data.content[0].status", is("OPEN")))
                .andExpect(jsonPath("$.data.content[0].priority", is("HIGH")));
    }

    private String createTicket(String title, String targetStatus, String priority) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/tickets")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "title", title,
                        "description", "List integration test",
                        "priority", priority
                ))))
                .andExpect(status().isCreated())
                .andReturn();

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>)
                mapper.readValue(result.getResponse().getContentAsString(), Map.class).get("data");
        String id = (String) data.get("id");

        if (!"OPEN".equals(targetStatus)) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                            "/api/v1/tickets/" + id + "/status?status=" + targetStatus)
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        return id;
    }
}


