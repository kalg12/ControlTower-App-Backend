package com.controltower.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Provides a com.fasterxml.jackson.databind.ObjectMapper bean for tests.
 * Spring Boot 4.x auto-configures tools.jackson.databind.json.JsonMapper (Jackson 3),
 * but test helpers still reference the Jackson 2 ObjectMapper class. This shim
 * registers a standalone instance so @Autowired ObjectMapper fields resolve.
 */
@TestConfiguration
public class TestObjectMapperConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
