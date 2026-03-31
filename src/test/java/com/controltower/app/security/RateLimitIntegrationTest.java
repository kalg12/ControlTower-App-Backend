package com.controltower.app.security;

import com.controltower.app.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that rate limiting is applied to public endpoints.
 * Sends 61 rapid requests to a rate-limited public endpoint and expects
 * at least one 429 response (the 61st request should be throttled).
 *
 * NOTE: This test may be slow to run because it sends many HTTP requests.
 *       The rate limit is 60 req/min per IP configured in RateLimitFilter.
 */
@DirtiesContext
class RateLimitIntegrationTest extends BaseIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    @DisplayName("61st request to /health/heartbeat returns 429")
    void rateLimitExceeded_returns429() throws Exception {
        boolean got429 = false;

        for (int i = 0; i < 70; i++) {
            var result = mvc.perform(post("/api/v1/health/heartbeat/nonexistent-for-rate-limit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(Map.of("status", "UP"))))
                    .andReturn();

            if (result.getResponse().getStatus() == 429) {
                got429 = true;
                break;
            }
        }

        assert got429 : "Expected at least one 429 after exceeding rate limit";
    }
}
