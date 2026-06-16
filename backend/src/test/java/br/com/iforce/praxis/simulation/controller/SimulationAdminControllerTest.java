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
    void publishDraftVersionRequiresApproval() throws Exception {
        mockMvc.perform(post("/api/v1/simulations/sim-publish-gate/versions/1/publish"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Versao precisa estar aprovada antes da publicacao."));
    }

    @Test
    @Sql(scripts = "/simulation-review-fixtures.sql")
    void reviewApproveAndPublishDraftVersion() throws Exception {
        mockMvc.perform(post("/api/v1/simulations/sim-review-flow/versions/1/submit-review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulationId").value("sim-review-flow"))
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.status").value("inReview"));

        mockMvc.perform(post("/api/v1/simulations/sim-review-flow/versions/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("approved"));

        mockMvc.perform(post("/api/v1/simulations/sim-review-flow/versions/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("published"))
                .andExpect(jsonPath("$.publishedAt").exists());

        mockMvc.perform(get("/api/v1/audit/simulations/sim-review-flow/versions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].eventType").value(hasItem("simulationVersionSubmittedForReview")))
                .andExpect(jsonPath("$[*].eventType").value(hasItem("simulationVersionApproved")))
                .andExpect(jsonPath("$[*].eventType").value(hasItem("simulationVersionPublished")));
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
    void monitorSeededVersionReturnsAttemptAndDeliveryIndicators() throws Exception {
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", "Bearer dev-company-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCandidateRequest("monitoring-document-1")))
                .andExpect(status().isOk());

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

    private String validCandidateRequest(String documentId) {
        return """
                {
                  "companyId": "empresa-123",
                  "documentId": "%s",
                  "testId": "sim-atendimento-caos",
                  "candidateName": "Thiago Souza",
                  "candidateEmail": "thiago@example.com",
                  "callbackUrl": "https://cliente.gupy.io/callback",
                  "resultWebhookUrl": "https://cliente.gupy.io/result-webhook",
                  "candidateType": "external",
                  "previousResult": "none"
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
