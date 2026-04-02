package com.controltower.app.shared.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc OpenAPI configuration.
 * Swagger UI available at /swagger-ui/index.html
 * API docs (JSON) at /v3/api-docs
 */
@Configuration
@SecurityScheme(
    name       = "bearerAuth",
    type       = SecuritySchemeType.HTTP,
    scheme     = "bearer",
    bearerFormat = "JWT",
    description = "Provide a valid JWT access token obtained from POST /api/v1/auth/login"
)
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI controlTowerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Control Tower API")
                        .description("""
                            Multi-tenant SaaS platform for monitoring, operating, and managing
                            clients that use POS systems and third-party services.

                            **Authentication:** Use `POST /api/v1/auth/login` to obtain a JWT,
                            then click **Authorize** and enter: `<your_access_token>`
                            """)
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Control Tower Team")
                                .email("dev@controltower.io"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://controltower.io")))
                // Relative URL: Swagger "Try it out" hits the same host that serves the UI (avoids CORS / wrong port).
                .servers(List.of(
                        new Server().url("/").description("Same origin (use when opening Swagger on the API host)"),
                        new Server().url("http://localhost:" + serverPort)
                                    .description("Direct backend (e.g. tools calling 8080 without UI)"),
                        new Server().url("https://api.controltower.io")
                                    .description("Production")
                ));
    }
}
