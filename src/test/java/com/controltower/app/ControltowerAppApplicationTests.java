package com.controltower.app;

import org.junit.jupiter.api.Test;

/**
 * Verifies that the Spring application context loads successfully with
 * real PostgreSQL and Redis containers (Testcontainers).
 */
class ControltowerAppApplicationTests extends BaseIntegrationTest {

    @Test
    void contextLoads() {
        // If this test passes, the full application context (Flyway, JPA, Redis,
        // Security, WebSocket) started without errors.
    }
}
