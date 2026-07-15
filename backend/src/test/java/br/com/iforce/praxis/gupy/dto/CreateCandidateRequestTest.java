package br.com.iforce.praxis.gupy.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateCandidateRequestTest {

    @Test
    void canonicalConstructorKeepsGupyJobAndCallback() {
        URI callbackUrl = URI.create("https://cliente.gupy.io/candidate-return");
        CreateCandidateRequest request = new CreateCandidateRequest(
                "empresa-123",
                "candidate-456",
                "sim-atendimento",
                "Candidato Teste",
                "candidato@example.com",
                901L,
                callbackUrl,
                URI.create("https://cliente.gupy.io/result-webhook"),
                BigDecimal.valueOf(1.5),
                CreateCandidateRequest.CandidateType.EXTERNAL,
                null
        );

        assertThat(request.jobId()).isEqualTo(901L);
        assertThat(request.callbackUrl()).isEqualTo(callbackUrl);
        assertThat(request.candidateType()).isEqualTo(CreateCandidateRequest.CandidateType.EXTERNAL);
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

        assertThat(request.jobId()).isNull();
        assertThat(request.callbackUrl()).isNull();
        assertThat(request.resultWebhookUrl()).hasToString("https://ats.example.com/result-webhook");
    }

    @Test
    void enumsExposeOnlyOfficialWireValues() {
        assertThat(CreateCandidateRequest.CandidateType.fromValue("internal"))
                .isEqualTo(CreateCandidateRequest.CandidateType.INTERNAL);
        assertThat(CreateCandidateRequest.CandidateType.fromValue("external"))
                .isEqualTo(CreateCandidateRequest.CandidateType.EXTERNAL);
        assertThat(CreateCandidateRequest.PreviousResult.fromValue("fail"))
                .isEqualTo(CreateCandidateRequest.PreviousResult.FAIL);
        assertThat(CreateCandidateRequest.PreviousResult.fromValue(null)).isNull();
    }

    @Test
    void enumsRejectValuesOutsideTheGupyContract() {
        assertThatThrownBy(() -> CreateCandidateRequest.CandidateType.fromValue("partner"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("candidate_type deve ser internal ou external.");
        assertThatThrownBy(() -> CreateCandidateRequest.PreviousResult.fromValue("none"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("previous_result deve ser fail ou null.");
        assertThatThrownBy(() -> CreateCandidateRequest.PreviousResult.fromValue("pass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("previous_result deve ser fail ou null.");
    }
}
