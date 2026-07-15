package br.com.iforce.praxis.gupy.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

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
                "external",
                null
        );

        assertThat(request.jobId()).isEqualTo(901L);
        assertThat(request.callbackUrl()).isEqualTo(callbackUrl);
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
}
