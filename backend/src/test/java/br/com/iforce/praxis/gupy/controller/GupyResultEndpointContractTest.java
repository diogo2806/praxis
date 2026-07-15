package br.com.iforce.praxis.gupy.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = {"/seed-simulation-fixture.sql", "/empresa-isolation-fixtures.sql"})
class GupyResultEndpointContractTest {

    private static final String EMPRESA1_AUTH = "Bearer empresa1-token";
    private static final String EMPRESA2_AUTH = "Bearer empresa2-token";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void resultEndpointUsesOnlyOfficialTopLevelFields() throws Exception {
        String resultId = createAttempt("contract-result-without-query");

        mockMvc.perform(get("/test/result/" + resultId)
                        .header("Authorization", EMPRESA1_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result_page_url").value(containsString("/results/")))
                .andExpect(jsonPath("$.result_page_url").value(not(containsString("/test/result/"))))
                .andExpect(jsonPath("$.result_page_url").value(not(containsString("?company_id="))))
                .andExpect(jsonPath("$.reliabilityLevel").doesNotExist())
                .andExpect(jsonPath("$.other_informations").doesNotExist());
    }

    @Test
    void resultEndpointKeepsTenantIsolationUsingBearerToken() throws Exception {
        String resultId = createAttempt("contract-result-tenant-isolation");

        mockMvc.perform(get("/test/result/" + resultId)
                        .header("Authorization", EMPRESA2_AUTH))
                .andExpect(status().isNotFound());
    }

    private String createAttempt(String documentId) throws Exception {
        MvcResult result = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", EMPRESA1_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company_id": "empresa-123",
                                  "document_id": "%s",
                                  "test_id": "sim-atendimento-caos",
                                  "name": "Candidato Contrato",
                                  "email": "contrato@example.com",
                                  "job_id": 100,
                                  "callback_url": "https://cliente.gupy.io/candidate-return",
                                  "result_webhook_url": "https://cliente.gupy.io/result-webhook",
                                  "candidate_type": "external",
                                  "previous_result": null
                                }
                                """.formatted(documentId)))
                .andExpect(status().isCreated())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.test_result_id");
    }
}
