package br.com.iforce.praxis.gupy.controller;

import br.com.iforce.praxis.auth.service.JwtService;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/seed-simulation-fixture.sql")
class GupyRealResultPagesTest {

    private static final String AUTHORIZATION = "Bearer empresa1-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CandidateAttemptRepository candidateAttemptRepository;

    @Test
    void resultPayloadPointsToRecruiterAndCandidateWebPages() throws Exception {
        MvcResult created = createCandidate("candidate-real-result-pages");
        String createdBody = created.getResponse().getContentAsString();
        String resultId = JsonPath.read(createdBody, "$.test_result_id");
        String testUrl = JsonPath.read(createdBody, "$.test_url");
        String testToken = testUrl.substring(testUrl.lastIndexOf('/') + 1);
        String attemptId = jwtService.parseCandidateAttemptToken(testToken).attemptId();

        MvcResult result = mockMvc.perform(get("/test/result/" + resultId)
                        .header("Authorization", AUTHORIZATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result_page_url")
                        .value("http://localhost:8080/results/" + attemptId))
                .andExpect(jsonPath("$.result_candidate_page_url")
                        .value(containsString("http://localhost:8080/candidato/")))
                .andExpect(jsonPath("$.result_candidate_page_url")
                        .value(containsString("/resultado")))
                .andExpect(jsonPath("$.result_page_url")
                        .value(not(containsString("/test/result/"))))
                .andExpect(jsonPath("$.result_candidate_page_url")
                        .value(not(containsString("/candidate/attempts/"))))
                .andReturn();

        String candidateResultUrl = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.result_candidate_page_url"
        );
        String candidateResultToken = candidateResultUrl
                .substring(candidateResultUrl.indexOf("/candidato/") + "/candidato/".length())
                .replace("/resultado", "");

        mockMvc.perform(get("/candidate/results/" + candidateResultToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avaliacaoNome").value("Cenario Seed de Teste"))
                .andExpect(jsonPath("$.status").value("nao_iniciada"))
                .andExpect(jsonPath("$.finalizado").value(false))
                .andExpect(jsonPath("$.redirectUrl").doesNotExist())
                .andExpect(content().string(not(containsString("candidateEmail"))))
                .andExpect(content().string(not(containsString("score"))))
                .andExpect(content().string(not(containsString("results"))));

        assertThat(candidateAttemptRepository.findById(attemptId).orElseThrow().getStatus())
                .isEqualTo(AttemptStatus.NOT_STARTED);

        mockMvc.perform(post("/candidate/attempts/" + testToken + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeId": "turno-1",
                                  "optionId": "opcao-equilibrada"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalizado").value(true));

        mockMvc.perform(get("/candidate/results/" + candidateResultToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("concluida"))
                .andExpect(jsonPath("$.finalizado").value(true))
                .andExpect(jsonPath("$.redirectUrl")
                        .value("https://cliente.gupy.io/candidate-return"))
                .andExpect(jsonPath("$.concluidoEm").exists());
    }

    private MvcResult createCandidate(String documentId) throws Exception {
        return mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company_id": "empresa-123",
                                  "document_id": "%s",
                                  "test_id": "sim-atendimento-caos",
                                  "name": "Thiago Souza",
                                  "email": "thiago@example.com",
                                  "job_id": 100,
                                  "callback_url": "https://cliente.gupy.io/candidate-return",
                                  "result_webhook_url": "https://cliente.gupy.io/result-webhook",
                                  "candidate_type": "external",
                                  "previous_result": null
                                }
                                """.formatted(documentId)))
                .andExpect(status().isCreated())
                .andReturn();
    }
}
