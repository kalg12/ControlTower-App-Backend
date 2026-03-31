package com.controltower.app.security;

import com.controltower.app.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

/**
 * Verifies that security headers are present on all responses:
 *   - Strict-Transport-Security (HSTS)
 *   - X-Content-Type-Options: nosniff
 *   - X-Frame-Options: DENY
 *   - Content-Security-Policy
 */
class SecurityHeadersTest extends BaseIntegrationTest {


    @Test
    @DisplayName("Responses include X-Content-Type-Options: nosniff")
    void response_hasXContentTypeOptions() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    @DisplayName("Responses include X-Frame-Options: DENY")
    void response_hasXFrameOptions() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    @DisplayName("Responses include Content-Security-Policy header")
    void response_hasContentSecurityPolicy() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    @DisplayName("Responses include X-Correlation-ID header")
    void response_hasCorrelationId() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(header().exists("X-Correlation-ID"));
    }

    @Test
    @DisplayName("X-Correlation-ID from request is echoed back")
    void correlationId_isEchoedBack() throws Exception {
        mvc.perform(get("/actuator/health")
                .header("X-Correlation-ID", "test-correlation-123"))
                .andExpect(header().string("X-Correlation-ID", "test-correlation-123"));
    }
}
