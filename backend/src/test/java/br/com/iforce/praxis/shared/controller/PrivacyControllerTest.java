package br.com.iforce.praxis.shared.controller;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.web.servlet.MockMvc;


import static org.hamcrest.Matchers.greaterThan;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class PrivacyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getComplianceReturnsLgpdOperationalPolicy() throws Exception {
        mockMvc.perform(get("/api/v1/privacy/compliance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalBases.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.retentionDays").value(180))
                .andExpect(jsonPath("$.controllerContact.controllerName").value("Controlador a ser informado pelo empresa"))
                .andExpect(jsonPath("$.reviewChannel").value("Canal de privacidade nao configurado para este empresa. Configure PRAXIS_PRIVACY_SERVICE_EMAIL ou PRAXIS_PRIVACY_SERVICE_URL antes de operar processos reais."))
                .andExpect(jsonPath("$.automatedDecisionWithoutReviewAllowed").value(false));
    }
}
