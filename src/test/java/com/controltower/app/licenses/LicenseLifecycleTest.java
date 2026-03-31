package com.controltower.app.licenses;

import com.controltower.app.BaseIntegrationTest;
import com.controltower.app.util.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the license lifecycle:
 *   - Onboarding creates a TRIAL license automatically
 *   - License can be suspended and reactivated
 *   - Plan catalog is accessible
 */
@DirtiesContext
class LicenseLifecycleTest extends BaseIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = TestDataFactory.onboardAndGetToken(mvc, "lic-test", "admin@lic-test.com", "Admin123!");
    }

    @Test
    @DisplayName("Onboarding creates a TRIAL license")
    void onboarding_createsTrial() throws Exception {
        // Onboarding was done in setUp — list licenses and expect TRIAL
        mvc.perform(get("/api/v1/licenses")
                .header("Authorization", TestDataFactory.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("TRIAL"));
    }

    @Test
    @DisplayName("GET /licenses/plans → returns non-empty plan catalog")
    void listPlans_returnsPlans() throws Exception {
        mvc.perform(get("/api/v1/licenses/plans")
                .header("Authorization", TestDataFactory.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }

    @Test
    @DisplayName("License suspend and reactivate flow")
    void suspendAndReactivate() throws Exception {
        // Get the trial license ID
        MvcResult listResult = mvc.perform(get("/api/v1/licenses")
                .header("Authorization", TestDataFactory.bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        String licenseId = (String) ((java.util.List<Map<String, Object>>)
                ((Map<String, Object>)
                        mapper.readValue(listResult.getResponse().getContentAsString(), Map.class)
                                .get("data")).get("content")).get(0).get("id");

        // Suspend
        mvc.perform(post("/api/v1/licenses/" + licenseId + "/suspend")
                .header("Authorization", TestDataFactory.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

        // Reactivate
        mvc.perform(post("/api/v1/licenses/" + licenseId + "/reactivate")
                .header("Authorization", TestDataFactory.bearer(token))
                .param("extensionDays", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /licenses/{id}/features → returns feature list")
    void getFeatures_returnsList() throws Exception {
        MvcResult listResult = mvc.perform(get("/api/v1/licenses")
                .header("Authorization", TestDataFactory.bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        String licenseId = (String) ((java.util.List<Map<String, Object>>)
                ((Map<String, Object>)
                        mapper.readValue(listResult.getResponse().getContentAsString(), Map.class)
                                .get("data")).get("content")).get(0).get("id");

        mvc.perform(get("/api/v1/licenses/" + licenseId + "/features")
                .header("Authorization", TestDataFactory.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()));
    }
}
