package br.com.iforce.praxis.simulation.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/seed-simulation-fixture.sql")
class SimulationAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listActiveSimulationsReturnsLatestVersionSummary() throws Exception {
        mockMvc.perform(get("/api/v1/simulations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(hasItem("sim-atendimento-caos")))
                .andExpect(jsonPath("$[?(@.id=='sim-atendimento-caos')].versionNumber").value(hasItem(1)))
                .andExpect(jsonPath("$[?(@.id=='sim-atendimento-caos')].status").value(hasItem("published")))
                .andExpect(jsonPath("$[?(@.id=='sim-atendimento-caos')].competencies").isArray())
                .andExpect(jsonPath("$[?(@.id=='sim-atendimento-caos')].attemptsCreated").exists())
                .andExpect(jsonPath("$[?(@.id=='sim-atendimento-caos')].completionRatePercent").exists());
    }

    @Test
    void validateSeededVersionIsPublishable() throws Exception {
        mockMvc.perform(get("/api/v1/simulations/sim-atendimento-caos/versions/1/validation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulationId").value("sim-atendimento-caos"))
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.publishable").value(true))
                .andExpect(jsonPath("$.issues", empty()));
    }

    @Test
    void createDraftSimulationFromBlueprintPersistsInitialDraft() throws Exception {
        mockMvc.perform(post("/api/v1/simulations/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Analista de Atendimento N2 - Teste",
                                  "description": "Teste direto de criacao de rascunho",
                                  "rootNodeId": "turno-1",
                                  "competencies": ["Empatia", "Comunicacao"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Analista de Atendimento N2 - Teste"))
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.competencies").value(hasItem("Empatia")))
                .andExpect(jsonPath("$.competencies").value(hasItem("Comunicacao")));
    }

    @Test
    void publishSeededVersionKeepsItPublished() throws Exception {
        mockMvc.perform(post("/api/v1/simulations/sim-atendimento-caos/versions/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulationId").value("sim-atendimento-caos"))
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.status").value("published"))
                .andExpect(jsonPath("$.publishedAt").exists());

        mockMvc.perform(get("/api/v1/audit/simulations/sim-atendimento-caos/versions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].eventType").value(hasItem("simulationVersionPublished")))
                .andExpect(jsonPath("$[*].aggregateType").value(hasItem("SimulationVersion")))
                .andExpect(jsonPath("$[*].aggregateId").value(hasItem("sim-atendimento-caos:v1")))
                .andExpect(jsonPath("$[*].metadata").value(hasItem(containsString("\"status\":\"published\""))));
    }

    @Test
    @Sql(scripts = "/simulation-review-fixtures.sql")
    void publishDraftVersionDirectly() throws Exception {
        mockMvc.perform(post("/api/v1/simulations/sim-publish-gate/versions/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulationId").value("sim-publish-gate"))
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.status").value("published"))
                .andExpect(jsonPath("$.publishedAt").exists());
    }

    @Test
    @Sql(scripts = "/simulation-review-fixtures.sql")
    void reviewApprovalEndpointsAreRemoved() throws Exception {
        mockMvc.perform(post("/api/v1/simulations/sim-review-flow/versions/1/submit-review"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/simulations/sim-review-flow/versions/1/approve"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/simulations/sim-review-flow/versions/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Nao aprovar\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Sql(scripts = "/simulation-review-fixtures.sql")
    void clonePublishedVersionCreatesDraftWithGraphAndNoAttempts() throws Exception {
        mockMvc.perform(post("/api/v1/simulations/sim-clone-source/versions/1/clone-draft"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulationId").value("sim-clone-source"))
                .andExpect(jsonPath("$.sourceVersionNumber").value(1))
                .andExpect(jsonPath("$.newVersionNumber").value(2))
                .andExpect(jsonPath("$.status").value("draft"));

        mockMvc.perform(get("/api/v1/simulations/sim-clone-source/versions/2/validation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishable").value(true))
                .andExpect(jsonPath("$.issues", empty()));

        mockMvc.perform(get("/api/v1/simulations/sim-clone-source/versions/2/monitoring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptsCreated").value(0));

        mockMvc.perform(get("/api/v1/audit/simulations/sim-clone-source/versions/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].eventType").value(hasItem("simulationVersionCloned")))
                .andExpect(jsonPath("$[*].metadata").value(hasItem(containsString("\"sourceVersionNumber\":1"))));
    }

    @Test
    @Sql(scripts = "/simulation-review-fixtures.sql")
    void authoringNodeCrudPersistsDraftChangesAndAuditEvents() throws Exception {
        MvcResult addNodeResult = mockMvc.perform(post("/api/v1/simulations/sim-review-flow/versions/1/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientMessage": "Mensagem criada pelo editor.",
                                  "timeLimitSeconds": 60
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String nodeId = addNodeResult.getResponse().getContentAsString();

        mockMvc.perform(put("/api/v1/simulations/sim-review-flow/versions/1/nodes/{nodeId}", nodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientMessage": "Mensagem atualizada pelo editor.",
                                  "timeLimitSeconds": 90
                                }
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/simulations/sim-review-flow/versions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[?(@.id=='" + nodeId + "')].clientMessage").value(hasItem("Mensagem atualizada pelo editor.")))
                .andExpect(jsonPath("$.nodes[?(@.id=='" + nodeId + "')].timeLimitSeconds").value(hasItem(90)));

        mockMvc.perform(delete("/api/v1/simulations/sim-review-flow/versions/1/nodes/{nodeId}", nodeId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/simulations/sim-review-flow/versions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[?(@.id=='" + nodeId + "')]").value(empty()));

        mockMvc.perform(get("/api/v1/audit/simulations/sim-review-flow/versions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].eventType").value(hasItem("simulationNodeAdded")))
                .andExpect(jsonPath("$[*].eventType").value(hasItem("simulationNodeUpdated")))
                .andExpect(jsonPath("$[*].eventType").value(hasItem("simulationNodeDeleted")))
                .andExpect(jsonPath("$[*].metadata").value(hasItem(containsString("\"nodeId\":\"" + nodeId + "\""))));
    }

    @Test
    @Sql(scripts = "/simulation-review-fixtures.sql")
    void authoringOptionCrudPersistsDraftChangesAndAuditEvents() throws Exception {
        MvcResult addOptionResult = mockMvc.perform(post("/api/v1/simulations/sim-review-flow/versions/1/nodes/turno-1/options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOptionRequest("Alternativa criada pelo editor.", 81, 73)))
                .andExpect(status().isCreated())
                .andReturn();
        String optionId = addOptionResult.getResponse().getContentAsString();

        mockMvc.perform(put("/api/v1/simulations/sim-review-flow/versions/1/nodes/turno-1/options/{optionId}", optionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "Alternativa atualizada pelo editor.",
                                  "competencyLevels": {
                                    "Empatia": 90,
                                    "Resolucao": 88
                                  },
                                  "isCritical": true,
                                  "resultingTone": "Tom atualizado."
                                }
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/simulations/sim-review-flow/versions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[?(@.id=='turno-1')].options[?(@.id=='" + optionId + "')].text")
                        .value(hasItem("Alternativa atualizada pelo editor.")))
                .andExpect(jsonPath("$.nodes[?(@.id=='turno-1')].options[?(@.id=='" + optionId + "')].isCritical")
                        .value(hasItem(true)));

        mockMvc.perform(delete("/api/v1/simulations/sim-review-flow/versions/1/nodes/turno-1/options/{optionId}", optionId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/simulations/sim-review-flow/versions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[?(@.id=='turno-1')].options[?(@.id=='" + optionId + "')]").value(empty()));

        mockMvc.perform(get("/api/v1/audit/simulations/sim-review-flow/versions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].eventType").value(hasItem("simulationOptionAdded")))
                .andExpect(jsonPath("$[*].eventType").value(hasItem("simulationOptionUpdated")))
                .andExpect(jsonPath("$[*].eventType").value(hasItem("simulationOptionDeleted")))
                .andExpect(jsonPath("$[*].metadata").value(hasItem(containsString("\"optionId\":\"" + optionId + "\""))));
    }

    @Test
    @Sql(scripts = "/simulation-review-fixtures.sql")
    void addOptionRejectsMoreThanFourOptionsInDraftNode() throws Exception {
        mockMvc.perform(post("/api/v1/simulations/sim-review-flow/versions/1/nodes/turno-1/options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOptionRequest("Terceira alternativa.", 78, 71)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/simulations/sim-review-flow/versions/1/nodes/turno-1/options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOptionRequest("Quarta alternativa.", 79, 72)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/simulations/sim-review-flow/versions/1/nodes/turno-1/options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOptionRequest("Quinta alternativa.", 80, 73)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cada turno pode ter no maximo 4 alternativas."));
    }

    @Test
    void gupyPreflightApprovesSeededVersion() throws Exception {
        mockMvc.perform(get("/api/v1/simulations/sim-atendimento-caos/versions/1/gupy-preflight"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulationId").value("sim-atendimento-caos"))
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.integrationActive").value(false))
                .andExpect(jsonPath("$.checks[*].code").value(hasItem("publicBaseUrl")))
                .andExpect(jsonPath("$.checks[*].code").value(hasItem("integrationToken")))
                .andExpect(jsonPath("$.checks[*].code").value(hasItem("simulationValidation")))
                .andExpect(jsonPath("$.checks[*].status").value(hasItem("ok")));
    }

    @Test
    void activateGupyIntegrationPersistsStateAndAuditEvent() throws Exception {
        mockMvc.perform(post("/api/v1/simulations/sim-atendimento-caos/versions/1/gupy-activation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulationId").value("sim-atendimento-caos"))
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.integrationActive").value(true))
                .andExpect(jsonPath("$.integrationActivatedAt").exists());

        mockMvc.perform(get("/api/v1/simulations/sim-atendimento-caos/versions/1/gupy-preflight"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.integrationActive").value(true))
                .andExpect(jsonPath("$.integrationActivatedAt").exists());

        mockMvc.perform(get("/api/v1/audit/simulations/sim-atendimento-caos/versions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].eventType").value(hasItem("simulationGupyIntegrationActivated")));
    }

    @Test
    @Sql(scripts = "/tenant-isolation-fixtures.sql")
    void archiveSimulationDoesNotCrossTenantBoundary() throws Exception {
        mockMvc.perform(delete("/api/v1/simulations/sim-tenant2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void monitorSeededVersionReturnsAttemptAndDeliveryIndicators() throws Exception {
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", "Bearer dev-company-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCandidateRequest("monitoring-document-1")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/simulations/sim-atendimento-caos/versions/1/monitoring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulationId").value("sim-atendimento-caos"))
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.attemptsCreated").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.attemptsNotStarted").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.completionRatePercent").exists())
                .andExpect(jsonPath("$.dropOffRatePercent").exists())
                .andExpect(jsonPath("$.deliveriesPending").exists())
                .andExpect(jsonPath("$.deliveriesRetrying").exists())
                .andExpect(jsonPath("$.deliveriesSent").exists())
                .andExpect(jsonPath("$.deliveriesDeadLetter").exists());
    }

    @Test
    @Sql(statements = {
            "UPDATE simulation_competencies SET target_score = 82 WHERE simulation_version_id = 1 AND name = 'Empatia'",
            "INSERT INTO candidate_attempts (id, tenant_id, company_id, result_id, simulation_id, simulation_version_id, simulation_version_number, idempotency_key, candidate_name, candidate_email, result_webhook_url, status, score, decision, human_review_required, company_result_string, created_at, started_at, finished_at, anonymized_at) VALUES ('attempt-match-1', 'tenant-1', 'empresa-123', 'result-match-1', 'sim-atendimento-caos', 1, 1, 'idem-match-1', 'Ana Costa', 'ana@example.com', 'https://cliente.gupy.io/result-webhook', 'COMPLETED', 85, 'RECOMMEND_INTERVIEW', FALSE, 'Resultado Ana', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)",
            "INSERT INTO candidate_attempts (id, tenant_id, company_id, result_id, simulation_id, simulation_version_id, simulation_version_number, idempotency_key, candidate_name, candidate_email, result_webhook_url, status, score, decision, human_review_required, company_result_string, created_at, started_at, finished_at, anonymized_at) VALUES ('attempt-match-2', 'tenant-1', 'empresa-123', 'result-match-2', 'sim-atendimento-caos', 1, 1, 'idem-match-2', 'Bruno Lima', 'bruno@example.com', 'https://cliente.gupy.io/result-webhook', 'COMPLETED', 85, 'RECOMMEND_INTERVIEW', FALSE, 'Resultado Bruno', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)",
            "INSERT INTO result_items (candidate_attempt_id, name, score, tier) VALUES ('attempt-match-1', 'Empatia', 94, 'MAJOR')",
            "INSERT INTO result_items (candidate_attempt_id, name, score, tier) VALUES ('attempt-match-1', 'Resolucao de conflito', 76, 'MAJOR')",
            "INSERT INTO result_items (candidate_attempt_id, name, score, tier) VALUES ('attempt-match-1', 'Aderencia a politica', 68, 'MINOR')",
            "INSERT INTO result_items (candidate_attempt_id, name, score, tier) VALUES ('attempt-match-2', 'Empatia', 72, 'MAJOR')",
            "INSERT INTO result_items (candidate_attempt_id, name, score, tier) VALUES ('attempt-match-2', 'Resolucao de conflito', 91, 'MAJOR')",
            "INSERT INTO result_items (candidate_attempt_id, name, score, tier) VALUES ('attempt-match-2', 'Aderencia a politica', 79, 'MINOR')"
    })
    void talentMatchReturnsBenchmarkAndSelectedCandidates() throws Exception {
        mockMvc.perform(get("/api/v1/simulations/sim-atendimento-caos/versions/1/talent-match")
                        .queryParam("attemptIds", "attempt-match-1,attempt-match-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulationId").value("sim-atendimento-caos"))
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.benchmark[?(@.competencyName=='Empatia')].targetScore").value(hasItem(82)))
                .andExpect(jsonPath("$.candidates[0].attemptId").value("attempt-match-1"))
                .andExpect(jsonPath("$.candidates[0].candidateName").value("Ana Costa"))
                .andExpect(jsonPath("$.candidates[0].generalScore").value(85))
                .andExpect(jsonPath("$.candidates[0].competencies[?(@.competencyName=='Empatia')].score").value(hasItem(94)))
                .andExpect(jsonPath("$.candidates[1].attemptId").value("attempt-match-2"))
                .andExpect(jsonPath("$.candidates[1].competencies[?(@.competencyName=='Resolucao de conflito')].score").value(hasItem(91)));
    }

    @Test
    @Sql(scripts = "/tenant-isolation-fixtures.sql")
    @Sql(statements = {
            "INSERT INTO candidate_attempts (id, tenant_id, company_id, result_id, simulation_id, simulation_version_id, simulation_version_number, idempotency_key, candidate_name, candidate_email, result_webhook_url, status, score, decision, human_review_required, company_result_string, created_at, started_at, finished_at, anonymized_at) VALUES ('attempt-match-safe', 'tenant-1', 'empresa-123', 'result-match-safe', 'sim-atendimento-caos', 1, 1, 'idem-match-safe', 'Carla Nunes', 'carla@example.com', 'https://cliente.gupy.io/result-webhook', 'COMPLETED', 80, 'RECOMMEND_INTERVIEW', FALSE, 'Resultado Carla', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)",
            "INSERT INTO candidate_attempts (id, tenant_id, company_id, result_id, simulation_id, simulation_version_id, simulation_version_number, idempotency_key, candidate_name, candidate_email, result_webhook_url, status, score, decision, human_review_required, company_result_string, created_at, started_at, finished_at, anonymized_at) VALUES ('attempt-match-tenant2', 'tenant-2', 'empresa-456', 'result-match-tenant2', 'sim-tenant2', 9001, 1, 'idem-match-tenant2', 'Outro Tenant', 'tenant2@example.com', 'https://cliente.gupy.io/result-webhook', 'COMPLETED', 88, 'RECOMMEND_INTERVIEW', FALSE, 'Resultado Tenant 2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)"
    })
    void talentMatchRejectsCrossTenantAttemptIds() throws Exception {
        mockMvc.perform(get("/api/v1/simulations/sim-atendimento-caos/versions/1/talent-match")
                        .queryParam("attemptIds", "attempt-match-safe,attempt-match-tenant2"))
                .andExpect(status().isForbidden());
    }

    private String validCandidateRequest(String documentId) {
        return """
                {
                  "company_id": "empresa-123",
                  "document_id": "%s",
                  "test_id": "sim-atendimento-caos",
                  "name": "Thiago Souza",
                  "email": "thiago@example.com",
                  "result_webhook_url": "https://cliente.gupy.io/result-webhook",
                  "candidate_type": "external",
                  "previous_result": "none"
                }
                """.formatted(documentId);
    }

    private String validOptionRequest(String text, int empathyScore, int resolutionScore) {
        return """
                {
                  "text": "%s",
                  "competencyLevels": {
                    "Empatia": %d,
                    "Resolucao": %d
                  },
                  "isCritical": false,
                  "resultingTone": "Tom adequado."
                }
                """.formatted(text, empathyScore, resolutionScore);
    }
}
