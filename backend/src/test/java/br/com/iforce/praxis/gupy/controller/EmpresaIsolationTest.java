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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que dados de uma empresa não vazam para outra pelos endpoints Gupy. A empresa-1
 * (company_id 1, token "empresa1-token") e a empresa-2 (company_id 2, token "empresa2-token")
 * possuem simulações publicadas distintas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = {"/seed-simulation-fixture.sql", "/empresa-isolation-fixtures.sql"})
class EmpresaIsolationTest {

    private static final String EMPRESA1_AUTH = "Bearer empresa1-token";
    private static final String EMPRESA2_AUTH = "Bearer empresa2-token";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publishedCatalogIsScopedToTheAuthenticatedEmpresa() throws Exception {
        mockMvc.perform(get("/test").header("Authorization", EMPRESA1_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_tests").value(2))
                .andExpect(jsonPath("$.payload[*].id").value(hasItem("sim-atendimento-caos")))
                .andExpect(jsonPath("$.payload[*].id").value(hasItem("sim-timeout-fallback")));

        mockMvc.perform(get("/test").header("Authorization", EMPRESA2_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_tests").value(1))
                .andExpect(jsonPath("$.payload[*].id").value(contains("sim-empresa2")));
    }

    @Test
    void aEmpresaCannotCreateAttemptOnAnotherEmpresasSimulation() throws Exception {
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", EMPRESA2_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(candidateRequest(2L, 4398157201L, "sim-atendimento-caos")))
                .andExpect(status().isNotFound());
    }

    @Test
    void resultLookupIsNotVisibleToAnotherConfiguredEmpresa() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", EMPRESA1_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(candidateRequest(1L, 4398157202L, "sim-atendimento-caos")))
                .andExpect(status().isCreated())
                .andReturn();
        String resultId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.test_result_id");

        mockMvc.perform(get("/test/result/" + resultId)
                        .header("Authorization", EMPRESA1_AUTH))
                .andExpect(status().isOk());

        mockMvc.perform(get("/test/result/" + resultId)
                        .header("Authorization", EMPRESA2_AUTH))
                .andExpect(status().isNotFound());
    }

    private String candidateRequest(long companyId, long documentId, String testId) {
        return """
                {
                  "company_id": %d,
                  "document_id": %d,
                  "test_id": "%s",
                  "name": "Maria Lima",
                  "email": "maria@example.com",
                  "callback_url": "https://cliente.gupy.io/candidate-return",
                  "result_webhook_url": "https://cliente.gupy.io/result-webhook",
                  "candidate_type": "external",
                  "previous_result": null
                }
                """.formatted(companyId, documentId, testId);
    }
}
