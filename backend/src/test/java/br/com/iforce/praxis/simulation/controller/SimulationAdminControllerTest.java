package br.com.iforce.praxis.simulation.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SimulationAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    void gupyPreflightApprovesSeededVersion() throws Exception {
        mockMvc.perform(get("/api/v1/simulations/sim-atendimento-caos/versions/1/gupy-preflight"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulationId").value("sim-atendimento-caos"))
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.checks[*].code").value(hasItem("publicBaseUrl")))
                .andExpect(jsonPath("$.checks[*].code").value(hasItem("integrationToken")))
                .andExpect(jsonPath("$.checks[*].code").value(hasItem("simulationValidation")))
                .andExpect(jsonPath("$.checks[*].status").value(hasItem("ok")));
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
}
