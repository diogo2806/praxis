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
 * Verifica que dados de um empresa não vazam para outro pelos endpoints Gupy. O empresa-1
 * (empresa-123, token "empresa1-token") e o empresa-2 (empresa-456, token "empresa2-token") possuem
 * simulações publicadas distintas; nenhum dos dois enxerga a simulação ou o resultado do outro.
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
        // empresa-1 só enxerga sua própria simulação.
        mockMvc.perform(get("/test").header("Authorization", EMPRESA1_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_tests").value(2))
                .andExpect(jsonPath("$.payload[*].id").value(hasItem("sim-atendimento-caos")))
                .andExpect(jsonPath("$.payload[*].id").value(hasItem("sim-timeout-fallback")));

        // empresa-2 (resolvido pelo hash do token) só enxerga a sua.
        mockMvc.perform(get("/test").header("Authorization", EMPRESA2_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_tests").value(1))
                .andExpect(jsonPath("$.payload[*].id").value(contains("sim-empresa2")));
    }

    @Test
    void aEmpresaCannotCreateAttemptOnAnotherEmpresasSimulation() throws Exception {
        // empresa-456 (empresa-2) tentando usar a simulação publicada do empresa-1.
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", EMPRESA2_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(candidateRequest("empresa-456", "doc-cross-empresa", "sim-atendimento-caos")))
                .andExpect(status().isNotFound());
    }

    @Test
    void resultLookupIsNotVisibleToAnotherConfiguredEmpresa() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", EMPRESA1_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(candidateRequest("empresa-123", "doc-isolation", "sim-atendimento-caos")))
                .andExpect(status().isCreated())
                .andReturn();
        String resultId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.test_result_id");

        // O dono (empresa-123) enxerga o resultado.
        mockMvc.perform(get("/test/result/" + resultId)
                        .header("Authorization", EMPRESA1_AUTH))
                .andExpect(status().isOk());

        // Outro empresa configurado (empresa-456) recebe 404, sem vazamento.
        mockMvc.perform(get("/test/result/" + resultId)
                        .header("Authorization", EMPRESA2_AUTH))
                .andExpect(status().isNotFound());
    }

    private String candidateRequest(String companyId, String documentId, String testId) {
        return """
                {
                  "company_id": "%s",
                  "document_id": "%s",
                  "test_id": "%s",
                  "name": "Maria Lima",
                  "email": "maria@example.com",
                  "callback_url": "https://cliente.gupy.io/candidate-return",
                  "result_webhook_url": "https://cliente.gupy.io/result-webhook",
                  "candidate_type": "external",
                  "previous_result": "none"
                }
                """.formatted(companyId, documentId, testId);
    }
}
