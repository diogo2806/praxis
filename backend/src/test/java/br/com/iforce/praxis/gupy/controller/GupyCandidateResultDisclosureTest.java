package br.com.iforce.praxis.gupy.controller;

import br.com.iforce.praxis.auth.service.JwtService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
class GupyCandidateResultDisclosureTest {

    private static final String AUTHORIZATION = "Bearer empresa1-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void exposesOnlySafeMajorResultsAfterCompletion() throws Exception {
        MvcResult created = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company_id": 1,
                                  "document_id": 4398157601,
                                  "test_id": "sim-atendimento-caos",
                                  "name": "Candidato Resultado",
                                  "email": "resultado@example.com",
                                  "job_id": 100,
                                  "callback_url": "https://cliente.gupy.io/candidate-return",
                                  "candidate_type": "external",
                                  "previous_result": "null"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String createdBody = created.getResponse().getContentAsString();
        String resultId = JsonPath.read(createdBody, "$.test_result_id");
        String testUrl = JsonPath.read(createdBody, "$.test_url");
        String testToken = testUrl.substring(testUrl.lastIndexOf('/') + 1);
        jwtService.parseCandidateAttemptToken(testToken);

        MvcResult externalResult = mockMvc.perform(get("/test/result/" + resultId)
                        .header("Authorization", AUTHORIZATION))
                .andExpect(status().isOk())
                .andReturn();
        String candidateResultUrl = JsonPath.read(
                externalResult.getResponse().getContentAsString(),
                "$.result_candidate_page_url"
        );
        String candidateResultToken = candidateResultUrl
                .substring(candidateResultUrl.indexOf("/candidato/") + "/candidato/".length())
                .replace("/resultado", "");

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
                .andExpect(jsonPath("$.resultados").isNotEmpty())
                .andExpect(jsonPath("$.resultados[0].titulo").isString())
                .andExpect(jsonPath("$.resultados[0].pontuacao").isNumber())
                .andExpect(jsonPath("$.resultados[0].resultado").value(containsString("%")))
                .andExpect(content().string(not(containsString("candidateEmail"))))
                .andExpect(content().string(not(containsString("answers"))));
    }
}
