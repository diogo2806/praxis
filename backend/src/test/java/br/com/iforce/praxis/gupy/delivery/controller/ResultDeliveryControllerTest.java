package br.com.iforce.praxis.gupy.delivery.controller;

import br.com.iforce.praxis.auth.service.JwtService;
import br.com.iforce.praxis.gupy.delivery.service.ResultWebhookClient;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/seed-simulation-fixture.sql")
class ResultDeliveryControllerTest {

    private static final String AUTHORIZATION = "Bearer empresa1-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResultWebhookClient resultWebhookClient;

    @Autowired
    private JwtService jwtService;

    @Test
    void completedAttemptCreatesPendingResultDelivery() throws Exception {
        String attemptId = createCompletedAttempt("delivery-pending");

        MvcResult deliveryResult = mockMvc.perform(get("/api/v1/gupy/result-deliveries?status=pending"))
                .andExpect(status().isOk())
                .andReturn();

        String deliveryBody = deliveryResult.getResponse().getContentAsString();
        List<String> statuses = JsonPath.read(deliveryBody, "$[?(@.attemptId == '" + attemptId + "')].status");
        List<Integer> attemptCounts = JsonPath.read(deliveryBody, "$[?(@.attemptId == '" + attemptId + "')].attemptCount");

        assertThat(statuses).containsExactly("pending");
        assertThat(attemptCounts).containsExactly(0);
    }

    @Test
    void listDeliveriesCanBeFilteredBySimulationVersion() throws Exception {
        String attemptId = createCompletedAttempt("delivery-filtered");

        MvcResult matchingResult = mockMvc.perform(get(
                        "/api/v1/gupy/result-deliveries?simulationId=sim-atendimento-caos&versionNumber=1"
                ))
                .andExpect(status().isOk())
                .andReturn();

        String matchingBody = matchingResult.getResponse().getContentAsString();
        List<String> matchingAttemptIds = JsonPath.read(matchingBody, "$[*].attemptId");
        assertThat(matchingAttemptIds).contains(attemptId);

        MvcResult emptyResult = mockMvc.perform(get(
                        "/api/v1/gupy/result-deliveries?simulationId=sim-atendimento-caos&versionNumber=99"
                ))
                .andExpect(status().isOk())
                .andReturn();

        String emptyBody = emptyResult.getResponse().getContentAsString();
        List<String> emptyAttemptIds = JsonPath.read(emptyBody, "$[*].attemptId");
        assertThat(emptyAttemptIds).doesNotContain(attemptId);
    }

    @Test
    void reprocessDeliveryMarksAsSentWhenWebhookAcceptsPayload() throws Exception {
        String attemptId = createCompletedAttempt("delivery-sent");
        Long deliveryId = findDeliveryId(attemptId, "pending");

        doNothing().when(resultWebhookClient)
                .postResult(eq("https://cliente.gupy.io/result-webhook"), any(TestResultResponse.class));

        mockMvc.perform(post("/api/v1/gupy/result-deliveries/" + deliveryId + "/reprocess"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivery.status").value("sent"))
                .andExpect(jsonPath("$.delivery.attemptCount").value(1))
                .andExpect(jsonPath("$.delivery.sentAt").exists());

        ArgumentCaptor<TestResultResponse> payloadCaptor = ArgumentCaptor.forClass(TestResultResponse.class);
        verify(resultWebhookClient).postResult(eq("https://cliente.gupy.io/result-webhook"), payloadCaptor.capture());

        TestResultResponse payload = payloadCaptor.getValue();
        assertThat(payload.testCode()).isEqualTo("sim-atendimento-caos");
        assertThat(payload.status()).isEqualTo("done");
        assertThat(payload.company_result_string()).contains("Pontuação geral: 100/100");
        assertThat(payload.results()).anySatisfy(result -> {
            assertThat(result.title()).isEqualTo("Empatia");
            assertThat(result.type_result()).isEqualTo("percentage");
            assertThat(result.tier()).isEqualTo("major");
        });
    }

    @Test
    void processReadyDeliveriesMarksPendingDeliveriesAsSent() throws Exception {
        String attemptId = createCompletedAttempt("delivery-process-ready");

        doNothing().when(resultWebhookClient)
                .postResult(eq("https://cliente.gupy.io/result-webhook"), any(TestResultResponse.class));

        mockMvc.perform(post("/api/v1/gupy/result-deliveries/process-ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        MvcResult deliveryResult = mockMvc.perform(get("/api/v1/gupy/result-deliveries?status=sent"))
                .andExpect(status().isOk())
                .andReturn();

        String deliveryBody = deliveryResult.getResponse().getContentAsString();
        List<String> statuses = JsonPath.read(deliveryBody, "$[?(@.attemptId == '" + attemptId + "')].status");
        assertThat(statuses).containsExactly("sent");
    }

    @Test
    void reprocessDeliveryRetriesAndMovesToDlqAfterFifthFailure() throws Exception {
        String attemptId = createCompletedAttempt("delivery-dlq");
        Long deliveryId = findDeliveryId(attemptId, "pending");

        doThrow(new IllegalStateException("HTTP 500"))
                .when(resultWebhookClient)
                .postResult(eq("https://cliente.gupy.io/result-webhook"), any(TestResultResponse.class));

        for (int attempt = 1; attempt <= 4; attempt++) {
            mockMvc.perform(post("/api/v1/gupy/result-deliveries/" + deliveryId + "/reprocess"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.delivery.status").value("retrying"))
                    .andExpect(jsonPath("$.delivery.attemptCount").value(attempt))
                    .andExpect(jsonPath("$.delivery.nextAttemptAt").exists());
        }

        mockMvc.perform(post("/api/v1/gupy/result-deliveries/" + deliveryId + "/reprocess"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivery.status").value("dlq"))
                .andExpect(jsonPath("$.delivery.attemptCount").value(5))
                .andExpect(jsonPath("$.delivery.lastError").value("HTTP 500"));
    }

    @Test
    void contractErrorMovesDeliveryStraightToDlqWithoutRetry() throws Exception {
        String attemptId = createCompletedAttempt("delivery-contract-error");
        Long deliveryId = findDeliveryId(attemptId, "pending");

        doThrow(new org.springframework.web.client.HttpClientErrorException(org.springframework.http.HttpStatus.BAD_REQUEST))
                .when(resultWebhookClient)
                .postResult(eq("https://cliente.gupy.io/result-webhook"), any(TestResultResponse.class));

        mockMvc.perform(post("/api/v1/gupy/result-deliveries/" + deliveryId + "/reprocess"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivery.status").value("dlq"))
                .andExpect(jsonPath("$.delivery.attemptCount").value(1))
                .andExpect(jsonPath("$.delivery.nextAttemptAt").doesNotExist());
    }

    private String createCompletedAttempt(String documentId) throws Exception {
        long numericDocumentId = Integer.toUnsignedLong(documentId.hashCode()) + 1L;
        MvcResult createResult = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company_id": 1,
                                  "document_id": %d,
                                  "test_id": "sim-atendimento-caos",
                                  "name": "Thiago Souza",
                                  "email": "thiago@example.com",
                                  "callback_url": "https://cliente.gupy.io/candidate-return",
                                  "result_webhook_url": "https://cliente.gupy.io/result-webhook",
                                  "candidate_type": "external",
                                  "previous_result": null
                                }
                                """.formatted(numericDocumentId)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String attemptId = attemptIdFromResponse(responseBody);

        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeId": "turno-1",
                                  "optionId": "opcao-equilibrada"
                                }
                                """))
                .andExpect(status().isOk());

        return attemptId;
    }

    private String attemptIdFromResponse(String responseBody) {
        String testUrl = JsonPath.read(responseBody, "$.test_url");
        String token = testUrl.substring(testUrl.lastIndexOf('/') + 1);
        return jwtService.parseCandidateAttemptToken(token).attemptId();
    }

    private Long findDeliveryId(String attemptId, String deliveryStatus) throws Exception {
        MvcResult deliveryResult = mockMvc.perform(get("/api/v1/gupy/result-deliveries?status=" + deliveryStatus))
                .andExpect(status().isOk())
                .andReturn();

        String deliveryBody = deliveryResult.getResponse().getContentAsString();
        List<Number> deliveryIds = JsonPath.read(deliveryBody, "$[?(@.attemptId == '" + attemptId + "')].id");
        assertThat(deliveryIds).hasSize(1);
        return deliveryIds.getFirst().longValue();
    }
}
