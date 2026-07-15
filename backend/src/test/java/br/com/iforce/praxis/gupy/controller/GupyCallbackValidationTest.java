package br.com.iforce.praxis.gupy.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/seed-simulation-fixture.sql")
class GupyCallbackValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsCallbackUrlWithoutHttpOrHttps() throws Exception {
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", "Bearer empresa1-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company_id": 1,
                                  "document_id": 10001,
                                  "test_id": "sim-atendimento-caos",
                                  "name": "Candidato Teste",
                                  "email": "candidato@example.com",
                                  "job_id": 100,
                                  "callback_url": "ftp://cliente.gupy.io/candidate-return"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
