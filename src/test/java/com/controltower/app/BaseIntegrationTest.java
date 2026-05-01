package com.controltower.app;

import com.controltower.app.shared.config.CorrelationIdFilter;
import com.controltower.app.shared.config.RateLimitFilter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import java.util.concurrent.Executors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for all integration tests.
 *
 * Starts PostgreSQL 17 and Redis 7 via Testcontainers.
 * Containers are shared across the test suite (static fields = started once).
 *
 * MockMvc is built manually from WebApplicationContext so we avoid the
 * @AutoConfigureMockMvc annotation whose package changed in Spring Boot 4.x.
 * Spring Security filters are applied via SecurityMockMvcConfigurers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "spring.task.scheduling.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.websocket.servlet.WebSocketMessagingAutoConfiguration",
        "spring.main.lazy-initialization=true",
        "spring.datasource.hikari.maximum-pool-size=20",
        "spring.datasource.hikari.minimum-idle=1",
        "spring.datasource.hikari.connection-timeout=5000",
        "spring.datasource.hikari.validation-timeout=1000",
        "spring.datasource.hikari.idle-timeout=0",
        "spring.datasource.hikari.max-lifetime=0",
        "spring.datasource.hikari.keepalive-time=30000"
})
@Import({TestObjectMapperConfig.class, TestAsyncConfig.class, BaseIntegrationTest.NoOpSchedulingConfig.class})
public abstract class BaseIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"))
                    .withDatabaseName("controltower_test")
                    .withUsername("controltower")
                    .withPassword("controltower123");

    @SuppressWarnings("resource")
    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379)
                    .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1));

    static {
        Startables.deepStart(POSTGRES, REDIS).join();
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CorrelationIdFilter correlationIdFilter;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    /** Shared MockMvc instance available to all subclass tests. */
    protected MockMvc mvc;

    @BeforeEach
    void setUpMockMvc() {
        mvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .addFilters(correlationIdFilter, rateLimitFilter)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @TestConfiguration
    static class NoOpSchedulingConfig {
        @Bean(destroyMethod = "shutdownNow")
        java.util.concurrent.ScheduledExecutorService disabledTestScheduler() {
            return Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName("disabled-test-scheduler");
                return thread;
            });
        }

        @Bean
        SchedulingConfigurer schedulingConfigurer(java.util.concurrent.ScheduledExecutorService disabledTestScheduler) {
            return taskRegistrar -> taskRegistrar.setScheduler(disabledTestScheduler);
        }
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username",  POSTGRES::getUsername);
        registry.add("spring.datasource.password",  POSTGRES::getPassword);

        // Redis (no password in test)
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379).toString());
        registry.add("spring.data.redis.password", () -> "");

        // Disable email in tests
        registry.add("spring.mail.host", () -> "");

        // Disable Stripe in tests
        registry.add("app.stripe.secret-key",      () -> "");
        registry.add("app.stripe.webhook-secret",  () -> "");
    }
}
