package br.com.iforce.praxis.simulation.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
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
}
