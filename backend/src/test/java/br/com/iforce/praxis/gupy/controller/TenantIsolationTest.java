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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garante que dados de um tenant nunca vazam para outro pelos endpoints Gupy. O tenant-1
 * (empresa-123, token "tenant1-token") e o tenant-2 (empresa-456, token "tenant2-token") possuem
 * simulações publicadas distintas; nenhum dos dois enxerga a simulação ou o resultado do outro.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = {"/seed-simulation-fixture.sql", "/tenant-isolation-fixtures.sql"})
class TenantIsolationTest {

    private static final String TENANT1_AUTH = "Bearer tenant1-token";
    private static final String TENANT2_AUTH = "Bearer tenant2-token";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publishedCatalogIsScopedToTheAuthenticatedTenant() throws Exception {
        // tenant-1 só enxerga sua própria simulação.
        mockMvc.perform(get("/test").header("Authorization", TENANT1_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_tests").value(1))
                .andExpect(jsonPath("$.payload[*].id").value(contains("sim-atendimento-caos")));

        // tenant-2 (resolvido pelo hash do token) só enxerga a sua.
        mockMvc.perform(get("/test").header("Authorization", TENANT2_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_tests").value(1))
                .andExpect(jsonPath("$.payload[*].id").value(contains("sim-tenant2")));
    }

    @Test
    void aTenantCannotCreateAttemptOnAnotherTenantsSimulation() throws Exception {
        // empresa-456 (tenant-2) tentando usar a simulação publicada do tenant-1.
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", TENANT2_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(candidateRequest("empresa-456", "doc-cross-tenant", "sim-atendimento-caos")))
                .andExpect(status().isNotFound());
    }

    @Test
    void resultLookupIsNotVisibleToAnotherConfiguredTenant() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", TENANT1_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(candidateRequest("empresa-123", "doc-isolation", "sim-atendimento-caos")))
                .andExpect(status().isCreated())
                .andReturn();
        String resultId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.test_result_id");

        // O dono (empresa-123) enxerga o resultado.
        mockMvc.perform(get("/test/result/" + resultId)
                        .header("Authorization", TENANT1_AUTH)
                        .param("company_id", "empresa-123"))
                .andExpect(status().isOk());

        // Outro tenant configurado (empresa-456) recebe 404, sem vazamento.
        mockMvc.perform(get("/test/result/" + resultId)
                        .header("Authorization", TENANT2_AUTH)
                        .param("company_id", "empresa-456"))
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
                  "result_webhook_url": "https://cliente.gupy.io/result-webhook",
                  "candidate_type": "external",
                  "previous_result": "none"
                }
                """.formatted(companyId, documentId, testId);
    }
}
