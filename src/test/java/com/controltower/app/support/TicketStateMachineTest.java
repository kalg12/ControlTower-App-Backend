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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;

/**
 * Tests the ticket state machine transitions.
 *
 * Valid transitions (fully bidirectional, any status → any other status):
 *   OPEN        → IN_PROGRESS | WAITING | RESOLVED | CLOSED
 *   IN_PROGRESS → WAITING | RESOLVED | CLOSED
 *   WAITING     → OPEN | IN_PROGRESS | RESOLVED | CLOSED
 *   RESOLVED    → OPEN | IN_PROGRESS | WAITING | CLOSED
 *   CLOSED      → OPEN | IN_PROGRESS | WAITING | RESOLVED
 *
 * Still invalid: IN_PROGRESS → OPEN (not in allowed set)
 */
@DirtiesContext
class TicketStateMachineTest extends BaseIntegrationTest {

    @Autowired ObjectMapper mapper;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = TestDataFactory.onboardAndGetToken(mvc, "ticket-sm", "admin@ticket-sm.com", "Admin123!");
    }

    @Test
    @DisplayName("OPEN → IN_PROGRESS is a valid transition")
    void openToInProgress_isValid() throws Exception {
        String ticketId = createTicket();
        transitionTicket(ticketId, "IN_PROGRESS")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("IN_PROGRESS")));
    }

    @Test
    @DisplayName("OPEN → RESOLVED is a valid transition")
    void openToResolved_isValid() throws Exception {
        String ticketId = createTicket();
        transitionTicket(ticketId, "RESOLVED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("RESOLVED")));
    }

    @Test
    @DisplayName("OPEN → CLOSED is a valid transition")
    void openToClosed_isValid() throws Exception {
        String ticketId = createTicket();
        transitionTicket(ticketId, "CLOSED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("CLOSED")));
    }

    @Test
    @DisplayName("IN_PROGRESS → WAITING is a valid transition")
    void inProgressToWaiting_isValid() throws Exception {
        String ticketId = createTicket();
        transitionTicket(ticketId, "IN_PROGRESS").andExpect(status().isOk());
        transitionTicket(ticketId, "WAITING")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("WAITING")));
    }

    @Test
    @DisplayName("WAITING → IN_PROGRESS is a valid transition")
    void waitingToInProgress_isValid() throws Exception {
        String ticketId = createTicket();
        transitionTicket(ticketId, "IN_PROGRESS").andExpect(status().isOk());
        transitionTicket(ticketId, "WAITING").andExpect(status().isOk());
        transitionTicket(ticketId, "IN_PROGRESS")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("IN_PROGRESS")));
    }

    @Test
    @DisplayName("RESOLVED → CLOSED is a valid transition")
    void resolvedToClosed_isValid() throws Exception {
        String ticketId = createTicket();
        transitionTicket(ticketId, "RESOLVED").andExpect(status().isOk());
        transitionTicket(ticketId, "CLOSED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("CLOSED")));
    }

    @Test
    @DisplayName("RESOLVED → OPEN (re-open) is a valid transition")
    void resolvedToOpen_isValid() throws Exception {
        String ticketId = createTicket();
        transitionTicket(ticketId, "RESOLVED").andExpect(status().isOk());
        transitionTicket(ticketId, "OPEN")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("OPEN")));
    }

    @Test
    @DisplayName("OPEN → WAITING is a valid transition (ticket awaiting client info)")
    void openToWaiting_isValid() throws Exception {
        String ticketId = createTicket();
        transitionTicket(ticketId, "WAITING")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("WAITING")));
    }

    @Test
    @DisplayName("CLOSED → RESOLVED is a valid transition (re-resolve)")
    void closedToResolved_isValid() throws Exception {
        String ticketId = createTicket();
        transitionTicket(ticketId, "CLOSED").andExpect(status().isOk());
        transitionTicket(ticketId, "RESOLVED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("RESOLVED")));
    }

    @Test
    @DisplayName("CLOSED → WAITING is a valid transition (post-support revert)")
    void closedToWaiting_isValid() throws Exception {
        String ticketId = createTicket();
        transitionTicket(ticketId, "CLOSED").andExpect(status().isOk());
        transitionTicket(ticketId, "WAITING")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("WAITING")));
    }

    @Test
    @DisplayName("IN_PROGRESS → OPEN is an INVALID transition → 400")
    void inProgressToOpen_isInvalid() throws Exception {
        String ticketId = createTicket();
        transitionTicket(ticketId, "IN_PROGRESS").andExpect(status().isOk());
        transitionTicket(ticketId, "OPEN")
                .andExpect(status().isBadRequest());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String createTicket() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/tickets")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "title",       "Test Ticket",
                    "description", "For state machine testing",
                    "priority",    "MEDIUM"
                ))))
                .andExpect(status().isCreated())
                .andReturn();

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>)
                mapper.readValue(result.getResponse().getContentAsString(), Map.class).get("data");
        return (String) data.get("id");
    }

    private org.springframework.test.web.servlet.ResultActions transitionTicket(
            String ticketId, String newStatus) throws Exception {
        return mvc.perform(patch("/api/v1/tickets/" + ticketId + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("status", newStatus))));
    }
}
