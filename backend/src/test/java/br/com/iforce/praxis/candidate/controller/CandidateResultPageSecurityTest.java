package br.com.iforce.praxis.candidate.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CandidateResultPageSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void candidateResultPageRejectsInvalidPublicToken() throws Exception {
        mockMvc.perform(get("/candidate/results/token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token público de candidato inválido."));
    }
}
