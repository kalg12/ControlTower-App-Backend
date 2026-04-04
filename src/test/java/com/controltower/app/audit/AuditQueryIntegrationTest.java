package com.controltower.app.audit;

import com.controltower.app.BaseIntegrationTest;
import com.controltower.app.util.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext
class AuditQueryIntegrationTest extends BaseIntegrationTest {

	@Autowired ObjectMapper mapper;

	private String token;

	@BeforeEach
	void setUp() throws Exception {
		token = TestDataFactory.onboardAndGetToken(mvc, "audit-query", "admin@audit-query.com", "Admin123!");
	}

	@Test
	@DisplayName("Audit endpoint returns 200 without filters")
	void auditWithoutFilters_returnsOk() throws Exception {
		createClient("Audit Client");

		mvc.perform(get("/api/v1/audit?page=0&size=20")
				.header("Authorization", TestDataFactory.bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success", is(true)))
				.andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(1)));
	}

	@Test
	@DisplayName("Audit compatibility alias returns 200")
	void auditAlias_returnsOk() throws Exception {
		createClient("Alias Client");

		mvc.perform(get("/api/v1/audit-logs?page=0&size=20")
				.header("Authorization", TestDataFactory.bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success", is(true)));
	}

	private void createClient(String name) throws Exception {
		mvc.perform(post("/api/v1/clients")
				.header("Authorization", TestDataFactory.bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(Map.of("name", name))))
				.andExpect(status().isCreated());
	}
}

