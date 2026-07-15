package br.com.iforce.praxis.gupy.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateCandidateRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void canonicalConstructorKeepsGupyJobAndCallback() {
        URI callbackUrl = URI.create("https://cliente.gupy.io/candidate-return");
        CreateCandidateRequest request = new CreateCandidateRequest(
                1L,
                4398157034L,
                "sim-atendimento",
                "Candidato Teste",
                "candidato@example.com",
                901L,
                callbackUrl,
                URI.create("https://cliente.gupy.io/result-webhook"),
                BigDecimal.valueOf(1.5),
                CreateCandidateRequest.CandidateType.EXTERNAL,
                CreateCandidateRequest.PreviousResult.FAIL
        );

        assertThat(request.companyId()).isEqualTo("1");
        assertThat(request.documentId()).isEqualTo("4398157034");
        assertThat(request.jobId()).isEqualTo(901L);
        assertThat(request.callbackUrl()).isEqualTo(callbackUrl);
        assertThat(request.candidateType()).isEqualTo(CreateCandidateRequest.CandidateType.EXTERNAL);
        assertThat(request.previousResult()).isEqualTo(CreateCandidateRequest.PreviousResult.FAIL);
    }

    @Test
    void compatibilityConstructorKeepsNonGupyProvidersWorking() {
        CreateCandidateRequest request = new CreateCandidateRequest(
                "empresa-123",
                "candidate-456",
                "sim-atendimento",
                "Candidato Teste",
                "candidato@example.com",
                URI.create("https://ats.example.com/result-webhook"),
                BigDecimal.ONE,
                null,
                null
        );

        assertThat(request.companyId()).isEqualTo("empresa-123");
        assertThat(request.documentId()).isEqualTo("candidate-456");
        assertThat(request.jobId()).isNull();
        assertThat(request.callbackUrl()).isNull();
        assertThat(request.resultWebhookUrl()).hasToString("https://ats.example.com/result-webhook");
    }

    @Test
    void deserializesOfficialNumericIdentifiersAndNormalizesNullPreviousResult() throws Exception {
        CreateCandidateRequest request = objectMapper.readValue("""
                {
                  "company_id": 1,
                  "document_id": 4398157034,
                  "test_id": "sim-atendimento",
                  "name": "Candidato Teste",
                  "email": "candidato@example.com",
                  "callback_url": "https://cliente.gupy.io/candidate-return",
                  "candidate_type": "internal",
                  "previous_result": "null"
                }
                """, CreateCandidateRequest.class);

        assertThat(request.contractCompanyId()).isEqualTo(1L);
        assertThat(request.contractDocumentId()).isEqualTo(4398157034L);
        assertThat(request.companyId()).isEqualTo("1");
        assertThat(request.documentId()).isEqualTo("4398157034");
        assertThat(request.candidateType()).isEqualTo(CreateCandidateRequest.CandidateType.INTERNAL);
        assertThat(request.previousResult()).isNull();
    }

    @Test
    void rejectsIdentifiersOutsideTheInt64JsonContract() {
        assertThatThrownBy(() -> objectMapper.readValue("""
                {
                  "company_id": "1",
                  "document_id": "4398157034",
                  "test_id": "sim-atendimento",
                  "name": "Candidato Teste",
                  "email": "candidato@example.com",
                  "callback_url": "https://cliente.gupy.io/candidate-return"
                }
                """, CreateCandidateRequest.class))
                .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void rejectsValuesOutsideOfficialEnums() {
        assertThatThrownBy(() -> objectMapper.readValue("""
                {
                  "company_id": 1,
                  "document_id": 4398157034,
                  "test_id": "sim-atendimento",
                  "name": "Candidato Teste",
                  "email": "candidato@example.com",
                  "callback_url": "https://cliente.gupy.io/candidate-return",
                  "candidate_type": "partner",
                  "previous_result": "none"
                }
                """, CreateCandidateRequest.class))
                .isInstanceOf(JsonProcessingException.class);
    }
}
